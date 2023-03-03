package eu.assembly11.jpaybysqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.iban4j.IbanUtil;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;


/**
 * @author Vladimír Kubala
 */
public class PayBySquareGenerator {

	private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("0.00");

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final String VALID_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

	private final PaymentData data;

	public PayBySquareGenerator(PaymentData data) {
		Objects.requireNonNull(data, "Data is required");
		this.data = data;
	}

	/**
	 * @return List of error messages, empty list when valid data
	 */
	public List<String> validate() {
		List<String> messages = new ArrayList<>();
		if (data.amount == null) {
			messages.add(generatePropertyRequiredText("amount"));
		}
		if (data.iban == null) {
			messages.add(generatePropertyRequiredText("iban"));
		}
		else {
			try {
				IbanUtil.validate(data.iban);
			} catch (Exception e) {
				messages.add(e.getMessage());
			}
		}
		if (data.currency == null) {
			messages.add(generatePropertyRequiredText("currency"));
		}
		if (data.beneficiaryName != null && data.beneficiaryName.length() > 70) {
			messages.add(generatePropertyMaxLengthText("beneficiaryName", 70));
		}
		if (data.beneficiaryAddr1 != null && data.beneficiaryAddr1.length() > 70) {
			messages.add(generatePropertyMaxLengthText("beneficiaryAddr1", 70));
		}
		if (data.beneficiaryAddr2 != null && data.beneficiaryAddr2.length() > 70) {
			messages.add(generatePropertyMaxLengthText("beneficiaryAddr2", 70));
		}
		if (data.note != null && data.note.length() > 140) {
			messages.add(generatePropertyMaxLengthText("note", 140));
		}
		if (data.variableSymbol != null && data.variableSymbol.length() > 10) {
			messages.add(generatePropertyMaxLengthText("variableSymbol", 10));
		}
		if (data.constantSymbol != null && data.constantSymbol.length() > 4) {
			messages.add(generatePropertyMaxLengthText("constantSymbol", 4));
		}
		if (data.specificSymbol != null && data.specificSymbol.length() > 10) {
			messages.add(generatePropertyMaxLengthText("specificSymbol", 10));
		}

		return messages;
	}

	private String generatePropertyRequiredText(String propertyName) {
		return String.format("Property %s is required", propertyName);
	}

	private String generatePropertyMaxLengthText(String propertyName, int length) {
		return String.format("Max length of property %s is %d", propertyName, length);
	}

	public BufferedImage generateSimplePaymentBarcodeImage(int width, int height) throws WriterException, IOException {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		String barcodeText = generateSimplePaymentBarcode();
		BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, width, height);
		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}

	public String generateSimplePaymentBarcode() throws IOException {
		List<String> messages = validate();
		if (!messages.isEmpty()) {
			throw new IOException(messages.get(0));
		}

		// basic data structure
		String dataStructure = '\t' +
				"1" + '\t' +    // payment
				"1" + '\t' +    // simple payment
				AMOUNT_FORMAT.format(data.amount) + '\t' +
				data.currency + '\t' +
				(data.date != null ? DATE_FORMAT.format(data.date) : "") + '\t' +
				data.variableSymbol + '\t' +
				data.constantSymbol + '\t' +
				((data.specificSymbol != null) ? data.specificSymbol : "") + '\t' +
				"" + '\t' +    //previous 3 entries in SEPA format, empty because already provided above
				((data.note != null) ? data.note : "") + '\t' +
				"1" + '\t' +    // to an account
				data.iban + '\t' +
				((data.swift != null) ? data.swift : "") + '\t' +
				"0" + '\t' +    // recursing
				"0" + '\t' +    // incaso
				((data.beneficiaryName != null) ? data.beneficiaryName : "") + '\t' +
				((data.beneficiaryAddr1 != null) ? data.beneficiaryAddr1 : "") + '\t' +
				((data.beneficiaryAddr2 != null) ? data.beneficiaryAddr2 : "");

		// Add a crc32 checksum
		CRC32 crc = new CRC32();
		crc.update(dataStructure.getBytes(StandardCharsets.UTF_8));
		byte[] crcBytes = longToBytes(crc.getValue());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(new byte[]{crcBytes[7], crcBytes[6], crcBytes[5], crcBytes[4]});
		outputStream.write(dataStructure.getBytes(StandardCharsets.UTF_8));
		byte[] dataWithCrc = outputStream.toByteArray();

		// lzma1 compress
		outputStream = new ByteArrayOutputStream();
		LZMA2Options options = new LZMA2Options();
		options.setLc(3);
		options.setLp(0);
		options.setPb(2);
		options.setDictSize(128 * 1024);
		LZMAOutputStream out = new LZMAOutputStream(outputStream, options, true);
		out.write(dataWithCrc);
		out.finish();
		byte[] compressed = outputStream.toByteArray();

		// 4) prepend length
		byte[] intBytes = intToBytes(dataWithCrc.length);
		outputStream = new ByteArrayOutputStream();
		outputStream.write(new byte[]{(byte) 0, (byte) 0, intBytes[3], intBytes[2]});
		outputStream.write(compressed);
		byte[] compressedWithLength = outputStream.toByteArray();

		// to binary string + append zeros up to size multiple of 5
		StringBuilder sbBinaryString = new StringBuilder();
		for (byte b : compressedWithLength) {
			sbBinaryString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
		}
		int remainder = sbBinaryString.toString().length() % 5;
		for (int i = 0; i < 5 - remainder; i++) {
			sbBinaryString.append("0");
		}
		String binaryString = sbBinaryString.toString();

		// Substitute each quintet of bits with corresponding character
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < binaryString.length(); i += 5) {
			int index = Integer.parseInt(binaryString.substring(i, i + 5), 2);
			result.append(VALID_CHARSET.charAt(index));
		}
		return result.toString();
	}

	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	private byte[] intToBytes(int x) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(x);
		return buffer.array();
	}

}
