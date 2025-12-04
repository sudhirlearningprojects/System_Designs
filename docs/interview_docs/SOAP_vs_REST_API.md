# SOAP vs REST API - Complete Comparison

## Quick Comparison Table

| Feature | SOAP | REST |
|---------|------|------|
| **Protocol** | Protocol (strict rules) | Architectural style (guidelines) |
| **Data Format** | XML only | JSON, XML, HTML, Plain Text |
| **Transport** | HTTP, SMTP, TCP, JMS | HTTP/HTTPS only |
| **State** | Stateless or Stateful | Stateless |
| **Security** | WS-Security (built-in) | HTTPS, OAuth, JWT |
| **Performance** | Slower (XML parsing) | Faster (lightweight JSON) |
| **Bandwidth** | High (verbose XML) | Low (compact JSON) |
| **Caching** | Not supported | HTTP caching supported |
| **Error Handling** | Standardized SOAP faults | HTTP status codes |
| **ACID Compliance** | Yes (built-in transactions) | No (application level) |
| **Use Case** | Enterprise, Banking, Payment | Web, Mobile, Microservices |

---

## 1. Architecture

### SOAP (Simple Object Access Protocol)
- **Type**: Protocol with strict standards
- **Structure**: Envelope → Header → Body → Fault
- **Contract**: WSDL (Web Services Description Language)
- **Messaging**: Request/Response, One-way, Async

### REST (Representational State Transfer)
- **Type**: Architectural style
- **Structure**: Resource-based URLs
- **Contract**: OpenAPI/Swagger (optional)
- **Messaging**: Request/Response over HTTP methods

---

## 2. Message Format

### SOAP Request Example

```xml
POST /PaymentService HTTP/1.1
Host: example.com
Content-Type: text/xml; charset=utf-8
SOAPAction: "http://example.com/ProcessPayment"

<?xml version="1.0"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Header>
    <auth:Authentication xmlns:auth="http://example.com/auth">
      <auth:Username>user123</auth:Username>
      <auth:Password>pass456</auth:Password>
    </auth:Authentication>
  </soap:Header>
  <soap:Body>
    <pay:ProcessPayment xmlns:pay="http://example.com/payment">
      <pay:Amount>100.00</pay:Amount>
      <pay:Currency>USD</pay:Currency>
      <pay:AccountNumber>1234567890</pay:AccountNumber>
    </pay:ProcessPayment>
  </soap:Body>
</soap:Envelope>
```

### SOAP Response Example

```xml
HTTP/1.1 200 OK
Content-Type: text/xml; charset=utf-8

<?xml version="1.0"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>
    <pay:ProcessPaymentResponse xmlns:pay="http://example.com/payment">
      <pay:TransactionId>TXN123456</pay:TransactionId>
      <pay:Status>SUCCESS</pay:Status>
      <pay:Message>Payment processed successfully</pay:Message>
    </pay:ProcessPaymentResponse>
  </soap:Body>
</soap:Envelope>
```

### REST Request Example

```http
POST /api/v1/payments HTTP/1.1
Host: example.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "amount": 100.00,
  "currency": "USD",
  "accountNumber": "1234567890"
}
```

### REST Response Example

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/payments/TXN123456

{
  "transactionId": "TXN123456",
  "status": "SUCCESS",
  "message": "Payment processed successfully",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 3. HTTP Methods

### SOAP
- Uses **POST** for all operations
- Operation defined in SOAP body
- No standard HTTP method usage

```xml
<!-- All operations use POST -->
POST /PaymentService
POST /PaymentService
POST /PaymentService
```

### REST
- Uses **HTTP methods** semantically
- Operation defined by HTTP method + URL

```http
GET    /api/v1/payments/123        # Read
POST   /api/v1/payments            # Create
PUT    /api/v1/payments/123        # Update (full)
PATCH  /api/v1/payments/123        # Update (partial)
DELETE /api/v1/payments/123        # Delete
```

---

## 4. Error Handling

### SOAP Fault

```xml
HTTP/1.1 500 Internal Server Error
Content-Type: text/xml

<?xml version="1.0"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>
    <soap:Fault>
      <soap:Code>
        <soap:Value>soap:Sender</soap:Value>
      </soap:Code>
      <soap:Reason>
        <soap:Text xml:lang="en">Invalid account number</soap:Text>
      </soap:Reason>
      <soap:Detail>
        <err:Error xmlns:err="http://example.com/error">
          <err:ErrorCode>ACC_001</err:ErrorCode>
          <err:Message>Account number must be 10 digits</err:Message>
        </err:Error>
      </soap:Detail>
    </soap:Fault>
  </soap:Body>
</soap:Envelope>
```

### REST Error

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": {
    "code": "ACC_001",
    "message": "Invalid account number",
    "details": "Account number must be 10 digits",
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

**HTTP Status Codes**:
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Authentication required
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## 5. Security

### SOAP Security (WS-Security)

```xml
<soap:Header>
  <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
    <wsse:UsernameToken>
      <wsse:Username>user123</wsse:Username>
      <wsse:Password Type="PasswordDigest">hashed_password</wsse:Password>
      <wsse:Nonce>random_nonce</wsse:Nonce>
      <wsu:Created>2024-01-15T10:30:00Z</wsu:Created>
    </wsse:UsernameToken>
    <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
      <!-- Digital signature for message integrity -->
    </ds:Signature>
    <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
      <!-- Encrypted message body -->
    </xenc:EncryptedData>
  </wsse:Security>
</soap:Header>
```

**Features**:
- Message-level encryption
- Digital signatures
- Username/password tokens
- SAML tokens
- Timestamp validation

### REST Security

```http
# 1. Basic Authentication
Authorization: Basic dXNlcjEyMzpwYXNzNDU2

# 2. Bearer Token (JWT)
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 3. OAuth 2.0
Authorization: Bearer <access_token>

# 4. API Key
X-API-Key: abc123def456

# 5. HTTPS (Transport-level encryption)
https://example.com/api/v1/payments
```

**Features**:
- HTTPS for transport security
- OAuth 2.0 for authorization
- JWT for stateless authentication
- API keys for simple auth
- CORS for cross-origin requests

---

## 6. WSDL vs OpenAPI

### SOAP WSDL (Web Services Description Language)

```xml
<?xml version="1.0"?>
<definitions name="PaymentService"
  targetNamespace="http://example.com/payment"
  xmlns:tns="http://example.com/payment"
  xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
  xmlns="http://schemas.xmlsoap.org/wsdl/">

  <types>
    <schema targetNamespace="http://example.com/payment">
      <element name="ProcessPaymentRequest">
        <complexType>
          <sequence>
            <element name="amount" type="decimal"/>
            <element name="currency" type="string"/>
            <element name="accountNumber" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element name="ProcessPaymentResponse">
        <complexType>
          <sequence>
            <element name="transactionId" type="string"/>
            <element name="status" type="string"/>
          </sequence>
        </complexType>
      </element>
    </schema>
  </types>

  <message name="ProcessPaymentInput">
    <part name="parameters" element="tns:ProcessPaymentRequest"/>
  </message>
  <message name="ProcessPaymentOutput">
    <part name="parameters" element="tns:ProcessPaymentResponse"/>
  </message>

  <portType name="PaymentPortType">
    <operation name="ProcessPayment">
      <input message="tns:ProcessPaymentInput"/>
      <output message="tns:ProcessPaymentOutput"/>
    </operation>
  </portType>

  <binding name="PaymentBinding" type="tns:PaymentPortType">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
    <operation name="ProcessPayment">
      <soap:operation soapAction="http://example.com/ProcessPayment"/>
      <input><soap:body use="literal"/></input>
      <output><soap:body use="literal"/></output>
    </operation>
  </binding>

  <service name="PaymentService">
    <port name="PaymentPort" binding="tns:PaymentBinding">
      <soap:address location="http://example.com/PaymentService"/>
    </port>
  </service>
</definitions>
```

### REST OpenAPI (Swagger)

```yaml
openapi: 3.0.0
info:
  title: Payment API
  version: 1.0.0
  description: REST API for payment processing

servers:
  - url: https://example.com/api/v1

paths:
  /payments:
    post:
      summary: Process a payment
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                amount:
                  type: number
                  format: decimal
                currency:
                  type: string
                accountNumber:
                  type: string
      responses:
        '201':
          description: Payment processed successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  transactionId:
                    type: string
                  status:
                    type: string
        '400':
          description: Invalid request
        '500':
          description: Server error

  /payments/{transactionId}:
    get:
      summary: Get payment details
      parameters:
        - name: transactionId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Payment details
        '404':
          description: Payment not found
```

---

## 7. Code Examples

### SOAP Client (Java)

```java
import javax.xml.soap.*;
import java.net.URL;

public class SOAPClient {
    
    public static void main(String[] args) throws Exception {
        SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = factory.createConnection();
        
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage message = messageFactory.createMessage();
        
        SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody body = envelope.getBody();
        
        SOAPElement payment = body.addChildElement("ProcessPayment", "pay", "http://example.com/payment");
        payment.addChildElement("Amount").addTextNode("100.00");
        payment.addChildElement("Currency").addTextNode("USD");
        payment.addChildElement("AccountNumber").addTextNode("1234567890");
        
        message.saveChanges();
        
        URL endpoint = new URL("http://example.com/PaymentService");
        SOAPMessage response = connection.call(message, endpoint);
        
        response.writeTo(System.out);
        connection.close();
    }
}
```

### REST Client (Java)

```java
import java.net.http.*;
import java.net.URI;

public class RESTClient {
    
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        String json = """
            {
              "amount": 100.00,
              "currency": "USD",
              "accountNumber": "1234567890"
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://example.com/api/v1/payments"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer token123")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
```

---

## 8. Performance Comparison

### Message Size

**SOAP Request**: ~800 bytes (XML)
```xml
<?xml version="1.0"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
  <soap:Body>
    <pay:ProcessPayment xmlns:pay="http://example.com/payment">
      <pay:Amount>100.00</pay:Amount>
      <pay:Currency>USD</pay:Currency>
      <pay:AccountNumber>1234567890</pay:AccountNumber>
    </pay:ProcessPayment>
  </soap:Body>
</soap:Envelope>
```

**REST Request**: ~120 bytes (JSON)
```json
{
  "amount": 100.00,
  "currency": "USD",
  "accountNumber": "1234567890"
}
```

**Bandwidth Savings**: REST is ~85% smaller

---

## 9. Caching

### SOAP
- **No built-in caching**
- All requests use POST (not cacheable)
- Must implement custom caching

### REST
- **HTTP caching supported**
- GET requests cacheable by default
- Cache headers: `Cache-Control`, `ETag`, `Last-Modified`

```http
# REST Response with caching
HTTP/1.1 200 OK
Cache-Control: max-age=3600
ETag: "abc123"
Last-Modified: Mon, 15 Jan 2024 10:30:00 GMT

{
  "transactionId": "TXN123456",
  "status": "SUCCESS"
}
```

---

## 10. When to Use What?

### Use SOAP When:
- **Enterprise applications** requiring strict contracts
- **Financial transactions** needing ACID compliance
- **High security** requirements (WS-Security)
- **Stateful operations** needed
- **Multiple transport protocols** (HTTP, SMTP, TCP)
- **Legacy system integration**

**Examples**:
- Banking systems
- Payment gateways
- Telecom services
- Government applications

### Use REST When:
- **Web and mobile applications**
- **Microservices architecture**
- **Public APIs** for third-party integration
- **High performance** and low latency needed
- **Scalability** is priority
- **Simple CRUD operations**

**Examples**:
- Social media APIs (Twitter, Facebook)
- E-commerce platforms
- Mobile backends
- IoT applications
- Cloud services (AWS, Azure)

---

## 11. Real-World Examples

### SOAP APIs
- **PayPal Payment API** (legacy)
- **Salesforce SOAP API**
- **Amazon MWS** (Marketplace Web Service)
- **SOAP-based banking APIs**

### REST APIs
- **Twitter API**
- **GitHub API**
- **Stripe Payment API**
- **Google Maps API**
- **AWS REST APIs**
- **Slack API**

---

## Summary

| Aspect | SOAP | REST |
|--------|------|------|
| **Complexity** | High | Low |
| **Learning Curve** | Steep | Easy |
| **Flexibility** | Rigid | Flexible |
| **Speed** | Slower | Faster |
| **Size** | Large | Small |
| **Tooling** | Complex | Simple |
| **Modern Usage** | Declining | Growing |
| **Best For** | Enterprise, Banking | Web, Mobile, Cloud |

**Trend**: REST has largely replaced SOAP for new applications due to simplicity, performance, and better fit for modern web/mobile architectures.
