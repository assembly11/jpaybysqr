package eu.assembly11.jpaybysqr;

import com.google.zxing.WriterException;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author Vladimír Kubala
 */
public class PayBySquareGeneratorTest {

	@Test
	public void baseTest() throws IOException, WriterException {

		String validIban = "NL32ABNA7023532722";

		String expectedCode = "0006U0006MTD91509C98Q7KC28RB8T1H51QO37SD8C20SIGRRE4KNK7G6SRULFU443GBEJN347H7VGPHHN4K1A5I763RMPDORQAI3UMH0CE5G4DTC7NV4KPS1VFUKGG56JKLSUN1T3S59QBTA91O3H07SH590IBCJ7QJUQ9TFVTCRKK00";
		PayBySquareGenerator generator = new PayBySquareGenerator(new PaymentData(BigDecimal.ONE, validIban, "BREXPLPWXXX",
				Date.from(LocalDate.of(2022, 10, 19).atStartOfDay(ZoneId.systemDefault()).toInstant()), "Foo",
				"EUR", "0123456789", "0308", "11", "bar", "Address-1", "Address-2"));
		Assert.assertEquals(0, generator.validate().size());
		Assert.assertEquals(expectedCode, generator.generateSimplePaymentBarcode());
		BufferedImage image = generator.generateSimplePaymentBarcodeImage(300, 300);
		Assert.assertNotNull(image);

		// invalid iban
		generator = new PayBySquareGenerator(new PaymentData(new BigDecimal("10.50"), "SK3400000000000000000000", "EUR", "1234567890", "0308", "Note!"));
		Assert.assertEquals(1, generator.validate().size());

		// valid
		generator = new PayBySquareGenerator(new PaymentData(new BigDecimal("10.50"), validIban, "EUR", "1234567890", "0308", "Note!"));
		Assert.assertEquals(0, generator.validate().size());

		// invalid - currency required
		generator = new PayBySquareGenerator(new PaymentData(new BigDecimal("10.50"), validIban, null, "1234567890", "0308", "Note!"));
		Assert.assertEquals(1, generator.validate().size());

		// invalid - variable symbol too long
		generator = new PayBySquareGenerator(new PaymentData(new BigDecimal("1500.50"), validIban, "EUR", "12345678901", "0308", "Note!"));
		Assert.assertEquals(1, generator.validate().size());

	}

}
