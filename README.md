# jpaybysqr

Allows to create simple payment using slovak Pay By Square QR payment standard.

## Example of use

```java
PayBySquareGenerator generator = new PayBySquareGenerator(new PaymentData(BigDecimal.ONE, "NL32ABNA7023532722", "EUR", "0123456789", "0308", "Note1"));
BufferedImage image = generator.generateSimplePaymentBarcodeImage(300, 300);
File outputfile = new File("payment.png");
ImageIO.write(image, "png", outputfile);
```