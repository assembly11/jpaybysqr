package eu.assembly11.jpaybysqr;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

public class PaymentData {

	/**
	 * Amount to pay
	 */
	public BigDecimal amount;

	/**
	 * ISO 13616 - up to 34 characters long international bank account number
	 */
	public String iban;

	/**
	 * BIC / Swift code - ISO 9362 8 or 11 characters long
	 */
	public String swift;

	public Date date;

	/**
	 * Max length 70
	 */
	public String beneficiaryName;

	/**
	 * 3 letter representation ISO 4217
	 */
	public String currency;

	/**
	 * Max length 10
	 */
	public String variableSymbol;

	/**
	 * Max length 4
	 */
	public String constantSymbol;

	/**
	 * Max length 10
	 */
	public String specificSymbol;

	/**
	 * Max length 140
	 */
	public String note;

	/**
	 * Max length 70
	 */
	public String beneficiaryAddr1;

	/**
	 * Max length 70
	 */
	public String beneficiaryAddr2;

	public PaymentData(BigDecimal amount, String iban, String swift, Date date, String beneficiaryName, String currency, String variableSymbol, String constantSymbol, String specificSymbol, String note, String beneficiaryAddr1, String beneficiaryAddr2) {
		this.amount = Objects.requireNonNull(amount, "Amount is required");
		this.iban = Objects.requireNonNull(iban, "IBAN is required");
		this.swift = swift;
		this.date = date;
		this.beneficiaryName = beneficiaryName;
		this.currency = currency;
		this.variableSymbol = variableSymbol;
		this.constantSymbol = constantSymbol;
		this.specificSymbol = specificSymbol;
		this.note = note;
		this.beneficiaryAddr1 = beneficiaryAddr1;
		this.beneficiaryAddr2 = beneficiaryAddr2;
	}

	public PaymentData(BigDecimal amount, String iban, String currency, String variableSymbol, String constantSymbol, String note) {
		this(amount, iban, null, new Date(), null, currency,
				variableSymbol, constantSymbol, null, note, null, null);
	}

}
