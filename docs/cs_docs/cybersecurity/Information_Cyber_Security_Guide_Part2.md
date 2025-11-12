# Information & Cyber Security Concepts Guide - Part 2

*Advanced security concepts covering software development security, auditing, and comprehensive security practices*

## Table of Contents (Part 2)
6. [Software Development Security](#software-development-security)
7. [Network Audit](#network-audit)
8. [Systems Audit](#systems-audit)
9. [Advanced Security Concepts](#advanced-security)
10. [MCQ Practice Questions](#mcq-questions)

---

## 6. Software Development Security {#software-development-security}

### Secure Software Development Lifecycle (SSDLC)

**SSDLC Phases**:
1. **Requirements**: Security requirements gathering
2. **Design**: Threat modeling, secure architecture
3. **Implementation**: Secure coding practices
4. **Testing**: Security testing, code review
5. **Deployment**: Secure configuration, hardening
6. **Maintenance**: Patch management, monitoring

### Secure Coding Practices

**Input Validation**:
```python
import re
import html
from typing import Union, List

class InputValidator:
    def __init__(self):
        self.patterns = {
            'email': r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$',
            'phone': r'^\+?1?-?\.?\s?\(?([0-9]{3})\)?[-.\s]?([0-9]{3})[-.\s]?([0-9]{4})$',
            'alphanumeric': r'^[a-zA-Z0-9]+$',
            'numeric': r'^[0-9]+$',
            'alpha': r'^[a-zA-Z]+$'
        }
        
        self.max_lengths = {
            'username': 50,
            'password': 128,
            'email': 254,
            'name': 100
        }
    
    def validate_input(self, input_value: str, input_type: str, 
                      max_length: int = None, required: bool = True) -> dict:
        """Comprehensive input validation"""
        result = {
            'valid': True,
            'errors': [],
            'sanitized_value': input_value
        }
        
        # Check if required
        if required and (not input_value or input_value.strip() == ''):
            result['valid'] = False
            result['errors'].append(f'{input_type} is required')
            return result
        
        # Check length
        if max_length and len(input_value) > max_length:
            result['valid'] = False
            result['errors'].append(f'{input_type} exceeds maximum length of {max_length}')
        
        # Pattern validation
        if input_type in self.patterns:
            if not re.match(self.patterns[input_type], input_value):
                result['valid'] = False
                result['errors'].append(f'Invalid {input_type} format')
        
        # Sanitization
        result['sanitized_value'] = self.sanitize_input(input_value, input_type)
        
        return result
    
    def sanitize_input(self, input_value: str, input_type: str) -> str:
        """Sanitize input to prevent injection attacks"""
        # HTML encode to prevent XSS
        sanitized = html.escape(input_value)
        
        # Remove potentially dangerous characters
        if input_type == 'sql_safe':
            # Remove SQL injection characters
            dangerous_chars = ["'", '"', ';', '--', '/*', '*/', 'xp_', 'sp_']
            for char in dangerous_chars:
                sanitized = sanitized.replace(char, '')
        
        elif input_type == 'filename':
            # Remove path traversal characters
            sanitized = sanitized.replace('..', '').replace('/', '').replace('\\', '')
            # Remove null bytes
            sanitized = sanitized.replace('\x00', '')
        
        return sanitized.strip()
    
    def validate_password_strength(self, password: str) -> dict:
        """Validate password strength"""
        result = {
            'valid': True,
            'score': 0,
            'errors': [],
            'suggestions': []
        }
        
        # Length check
        if len(password) < 8:
            result['valid'] = False
            result['errors'].append('Password must be at least 8 characters long')
        else:
            result['score'] += 1
        
        # Complexity checks
        if re.search(r'[a-z]', password):
            result['score'] += 1
        else:
            result['suggestions'].append('Add lowercase letters')
        
        if re.search(r'[A-Z]', password):
            result['score'] += 1
        else:
            result['suggestions'].append('Add uppercase letters')
        
        if re.search(r'[0-9]', password):
            result['score'] += 1
        else:
            result['suggestions'].append('Add numbers')
        
        if re.search(r'[!@#$%^&*(),.?":{}|<>]', password):
            result['score'] += 1
        else:
            result['suggestions'].append('Add special characters')
        
        # Common password check
        common_passwords = ['password', '123456', 'qwerty', 'admin', 'letmein']
        if password.lower() in common_passwords:
            result['valid'] = False
            result['errors'].append('Password is too common')
        
        # Strength rating
        if result['score'] >= 4:
            result['strength'] = 'Strong'
        elif result['score'] >= 3:
            result['strength'] = 'Medium'
        else:
            result['strength'] = 'Weak'
            result['valid'] = False
        
        return result

# Usage example
validator = InputValidator()

# Test input validation
email_result = validator.validate_input('user@example.com', 'email')
print(f"Email validation: {email_result}")

# Test password strength
password_result = validator.validate_password_strength('MySecureP@ss123')
print(f"Password strength: {password_result}")
```

**SQL Injection Prevention**:
```python
import sqlite3
from typing import List, Tuple, Any

class SecureDatabase:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.connection = sqlite3.connect(db_path)
        self.setup_database()
    
    def setup_database(self):
        """Setup database with sample tables"""
        cursor = self.connection.cursor()
        
        # Users table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # Products table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                description TEXT,
                category_id INTEGER
            )
        ''')
        
        self.connection.commit()
    
    def create_user(self, username: str, email: str, password_hash: str) -> bool:
        """Secure user creation using parameterized queries"""
        try:
            cursor = self.connection.cursor()
            cursor.execute(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                (username, email, password_hash)
            )
            self.connection.commit()
            return True
        except sqlite3.IntegrityError:
            return False
    
    def authenticate_user(self, username: str, password_hash: str) -> dict:
        """Secure user authentication"""
        cursor = self.connection.cursor()
        cursor.execute(
            "SELECT id, username, email FROM users WHERE username = ? AND password_hash = ?",
            (username, password_hash)
        )
        
        result = cursor.fetchone()
        if result:
            return {
                'authenticated': True,
                'user_id': result[0],
                'username': result[1],
                'email': result[2]
            }
        else:
            return {'authenticated': False}
    
    def search_products(self, search_term: str, category_id: int = None) -> List[Tuple]:
        """Secure product search"""
        cursor = self.connection.cursor()
        
        if category_id:
            cursor.execute(
                "SELECT * FROM products WHERE name LIKE ? AND category_id = ?",
                (f'%{search_term}%', category_id)
            )
        else:
            cursor.execute(
                "SELECT * FROM products WHERE name LIKE ?",
                (f'%{search_term}%',)
            )
        
        return cursor.fetchall()
    
    def get_user_orders(self, user_id: int, limit: int = 10) -> List[Tuple]:
        """Get user orders with pagination"""
        cursor = self.connection.cursor()
        cursor.execute(
            "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            (user_id, limit)
        )
        
        return cursor.fetchall()
    
    def execute_safe_query(self, query: str, params: Tuple = ()) -> List[Tuple]:
        """Execute parameterized query safely"""
        # Whitelist allowed operations
        allowed_operations = ['SELECT', 'INSERT', 'UPDATE', 'DELETE']
        
        query_upper = query.strip().upper()
        if not any(query_upper.startswith(op) for op in allowed_operations):
            raise ValueError("Query operation not allowed")
        
        # Prevent multiple statements
        if ';' in query and query.count(';') > 1:
            raise ValueError("Multiple statements not allowed")
        
        cursor = self.connection.cursor()
        cursor.execute(query, params)
        
        if query_upper.startswith('SELECT'):
            return cursor.fetchall()
        else:
            self.connection.commit()
            return []

# Example of vulnerable vs secure code
class VulnerableExample:
    """DO NOT USE - This shows vulnerable patterns"""
    
    def vulnerable_search(self, search_term: str) -> str:
        # VULNERABLE: String concatenation
        query = f"SELECT * FROM products WHERE name = '{search_term}'"
        # This allows SQL injection like: '; DROP TABLE products; --
        return query
    
    def vulnerable_login(self, username: str, password: str) -> str:
        # VULNERABLE: Direct string formatting
        query = "SELECT * FROM users WHERE username = '{}' AND password = '{}'".format(username, password)
        # This allows bypass like: admin' OR '1'='1' --
        return query

# Secure implementation example
secure_db = SecureDatabase(':memory:')

# Safe operations
user_created = secure_db.create_user('testuser', 'test@example.com', 'hashed_password')
print(f"User created: {user_created}")

auth_result = secure_db.authenticate_user('testuser', 'hashed_password')
print(f"Authentication: {auth_result}")
```

**Cross-Site Scripting (XSS) Prevention**:
```python
import html
import re
import bleach
from urllib.parse import quote, unquote

class XSSProtection:
    def __init__(self):
        # Allowed HTML tags and attributes
        self.allowed_tags = [
            'p', 'br', 'strong', 'em', 'u', 'ol', 'ul', 'li',
            'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote'
        ]
        
        self.allowed_attributes = {
            '*': ['class'],
            'a': ['href', 'title'],
            'img': ['src', 'alt', 'width', 'height']
        }
        
        # Dangerous patterns
        self.xss_patterns = [
            r'<script[^>]*>.*?</script>',
            r'javascript:',
            r'vbscript:',
            r'onload\s*=',
            r'onerror\s*=',
            r'onclick\s*=',
            r'onmouseover\s*=',
            r'<iframe[^>]*>.*?</iframe>',
            r'<object[^>]*>.*?</object>',
            r'<embed[^>]*>.*?</embed>'
        ]
    
    def sanitize_html(self, html_content: str) -> str:
        """Sanitize HTML content using bleach library"""
        # Clean HTML with allowed tags and attributes
        cleaned = bleach.clean(
            html_content,
            tags=self.allowed_tags,
            attributes=self.allowed_attributes,
            strip=True
        )
        
        return cleaned
    
    def escape_html(self, text: str) -> str:
        """HTML escape user input"""
        return html.escape(text, quote=True)
    
    def validate_url(self, url: str) -> bool:
        """Validate URL to prevent XSS through links"""
        if not url:
            return False
        
        # Check for dangerous protocols
        dangerous_protocols = ['javascript:', 'vbscript:', 'data:', 'file:']
        url_lower = url.lower().strip()
        
        for protocol in dangerous_protocols:
            if url_lower.startswith(protocol):
                return False
        
        # Allow only http, https, and relative URLs
        if url_lower.startswith(('http://', 'https://', '//', '/')):
            return True
        
        return False
    
    def detect_xss_attempt(self, input_text: str) -> dict:
        """Detect potential XSS attempts"""
        result = {
            'is_malicious': False,
            'detected_patterns': [],
            'risk_score': 0
        }
        
        input_lower = input_text.lower()
        
        for pattern in self.xss_patterns:
            matches = re.findall(pattern, input_lower, re.IGNORECASE | re.DOTALL)
            if matches:
                result['is_malicious'] = True
                result['detected_patterns'].append(pattern)
                result['risk_score'] += 10
        
        # Check for encoded attempts
        try:
            decoded = unquote(input_text)
            if decoded != input_text:
                for pattern in self.xss_patterns:
                    if re.search(pattern, decoded.lower(), re.IGNORECASE):
                        result['is_malicious'] = True
                        result['detected_patterns'].append(f"URL-encoded: {pattern}")
                        result['risk_score'] += 15
        except:
            pass
        
        return result
    
    def create_csp_header(self, nonce: str = None) -> str:
        """Create Content Security Policy header"""
        csp_directives = [
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "connect-src 'self'",
            "font-src 'self'",
            "object-src 'none'",
            "media-src 'self'",
            "frame-src 'none'",
            "base-uri 'self'",
            "form-action 'self'"
        ]
        
        if nonce:
            # Replace unsafe-inline with nonce for scripts
            csp_directives[1] = f"script-src 'self' 'nonce-{nonce}'"
        
        return "; ".join(csp_directives)

# Usage example
xss_protection = XSSProtection()

# Test malicious inputs
malicious_inputs = [
    "<script>alert('XSS')</script>",
    "<img src='x' onerror='alert(1)'>",
    "javascript:alert('XSS')",
    "<iframe src='javascript:alert(1)'></iframe>",
    "%3Cscript%3Ealert('encoded')%3C/script%3E"
]

for malicious_input in malicious_inputs:
    # Detect XSS attempt
    detection_result = xss_protection.detect_xss_attempt(malicious_input)
    print(f"Input: {malicious_input}")
    print(f"Malicious: {detection_result['is_malicious']}")
    print(f"Risk Score: {detection_result['risk_score']}")
    
    # Sanitize
    sanitized = xss_protection.sanitize_html(malicious_input)
    escaped = xss_protection.escape_html(malicious_input)
    
    print(f"Sanitized: {sanitized}")
    print(f"Escaped: {escaped}")
    print("---")

# Generate CSP header
csp_header = xss_protection.create_csp_header()
print(f"CSP Header: {csp_header}")
```

### Secure Authentication Implementation

**JWT Token Security**:
```python
import jwt
import hashlib
import secrets
import time
from datetime import datetime, timedelta
from typing import Dict, Optional

class SecureJWTManager:
    def __init__(self, secret_key: str = None):
        self.secret_key = secret_key or secrets.token_urlsafe(32)
        self.algorithm = 'HS256'
        self.access_token_expire = 900  # 15 minutes
        self.refresh_token_expire = 604800  # 7 days
        self.blacklisted_tokens = set()
    
    def generate_tokens(self, user_id: str, username: str, roles: list = None) -> Dict[str, str]:
        """Generate access and refresh tokens"""
        current_time = datetime.utcnow()
        
        # Access token payload
        access_payload = {
            'user_id': user_id,
            'username': username,
            'roles': roles or [],
            'type': 'access',
            'iat': current_time,
            'exp': current_time + timedelta(seconds=self.access_token_expire),
            'jti': secrets.token_urlsafe(16)  # JWT ID for blacklisting
        }
        
        # Refresh token payload
        refresh_payload = {
            'user_id': user_id,
            'type': 'refresh',
            'iat': current_time,
            'exp': current_time + timedelta(seconds=self.refresh_token_expire),
            'jti': secrets.token_urlsafe(16)
        }
        
        access_token = jwt.encode(access_payload, self.secret_key, algorithm=self.algorithm)
        refresh_token = jwt.encode(refresh_payload, self.secret_key, algorithm=self.algorithm)
        
        return {
            'access_token': access_token,
            'refresh_token': refresh_token,
            'token_type': 'Bearer',
            'expires_in': self.access_token_expire
        }
    
    def verify_token(self, token: str, token_type: str = 'access') -> Optional[Dict]:
        """Verify and decode JWT token"""
        try:
            # Check if token is blacklisted
            if token in self.blacklisted_tokens:
                return None
            
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            
            # Verify token type
            if payload.get('type') != token_type:
                return None
            
            # Check expiration
            if datetime.utcfromtimestamp(payload['exp']) < datetime.utcnow():
                return None
            
            return payload
            
        except jwt.InvalidTokenError:
            return None
    
    def refresh_access_token(self, refresh_token: str) -> Optional[Dict[str, str]]:
        """Generate new access token using refresh token"""
        payload = self.verify_token(refresh_token, 'refresh')
        
        if not payload:
            return None
        
        # Generate new access token
        return self.generate_tokens(
            payload['user_id'],
            payload.get('username', ''),
            payload.get('roles', [])
        )
    
    def blacklist_token(self, token: str) -> bool:
        """Add token to blacklist"""
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            self.blacklisted_tokens.add(token)
            return True
        except jwt.InvalidTokenError:
            return False
    
    def validate_token_claims(self, token: str, required_roles: list = None) -> bool:
        """Validate token claims and roles"""
        payload = self.verify_token(token)
        
        if not payload:
            return False
        
        # Check required roles
        if required_roles:
            user_roles = payload.get('roles', [])
            if not any(role in user_roles for role in required_roles):
                return False
        
        return True

# Usage example
jwt_manager = SecureJWTManager()

# Generate tokens
tokens = jwt_manager.generate_tokens('user123', 'john_doe', ['user', 'admin'])
print(f"Access Token: {tokens['access_token'][:50]}...")

# Verify token
payload = jwt_manager.verify_token(tokens['access_token'])
print(f"Token payload: {payload}")

# Validate roles
has_admin = jwt_manager.validate_token_claims(tokens['access_token'], ['admin'])
print(f"Has admin role: {has_admin}")
```

---

## 7. Network Audit {#network-audit}

### Network Security Assessment

**Network Discovery and Mapping**:
```python
import socket
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from typing import List, Dict

class NetworkAuditor:
    def __init__(self):
        self.discovered_hosts = []
        self.open_ports = {}
        self.services = {}
        self.vulnerabilities = []
    
    def ping_host(self, host: str) -> bool:
        """Ping a host to check if it's alive"""
        try:
            # Use ping command
            result = subprocess.run(
                ['ping', '-c', '1', '-W', '1000', host],
                capture_output=True,
                text=True,
                timeout=2
            )
            return result.returncode == 0
        except:
            return False
    
    def scan_network_range(self, network_base: str, start_ip: int = 1, end_ip: int = 254) -> List[str]:
        """Scan a network range for live hosts"""
        live_hosts = []
        
        def check_host(ip):
            host = f"{network_base}.{ip}"
            if self.ping_host(host):
                live_hosts.append(host)
                print(f"Host {host} is alive")
        
        # Use threading for faster scanning
        with ThreadPoolExecutor(max_workers=50) as executor:
            executor.map(check_host, range(start_ip, end_ip + 1))
        
        self.discovered_hosts = live_hosts
        return live_hosts
    
    def port_scan(self, host: str, ports: List[int] = None) -> Dict[int, str]:
        """Scan ports on a specific host"""
        if ports is None:
            # Common ports
            ports = [21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 1433, 3306, 3389, 5432, 8080]
        
        open_ports = {}
        
        def scan_port(port):
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(1)
                result = sock.connect_ex((host, port))
                
                if result == 0:
                    try:
                        service = socket.getservbyport(port)
                    except:
                        service = "unknown"
                    
                    open_ports[port] = service
                    print(f"Port {port} ({service}) is open on {host}")
                
                sock.close()
            except:
                pass
        
        with ThreadPoolExecutor(max_workers=100) as executor:
            executor.map(scan_port, ports)
        
        self.open_ports[host] = open_ports
        return open_ports
    
    def service_detection(self, host: str, port: int) -> Dict[str, str]:
        """Detect service version and details"""
        service_info = {
            'service': 'unknown',
            'version': 'unknown',
            'banner': ''
        }
        
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5)
            sock.connect((host, port))
            
            # Try to grab banner
            if port in [21, 22, 23, 25, 110, 143]:  # Services that send banners
                banner = sock.recv(1024).decode('utf-8', errors='ignore').strip()
                service_info['banner'] = banner
                
                # Parse common service banners
                if port == 22 and 'SSH' in banner:
                    service_info['service'] = 'SSH'
                    if 'OpenSSH' in banner:
                        version_start = banner.find('OpenSSH_') + 8
                        version_end = banner.find(' ', version_start)
                        service_info['version'] = banner[version_start:version_end]
                
                elif port == 80 or port == 8080:
                    sock.send(b'GET / HTTP/1.1\r\nHost: ' + host.encode() + b'\r\n\r\n')
                    response = sock.recv(1024).decode('utf-8', errors='ignore')
                    if 'Server:' in response:
                        server_line = [line for line in response.split('\n') if 'Server:' in line][0]
                        service_info['service'] = 'HTTP'
                        service_info['version'] = server_line.split('Server:')[1].strip()
            
            sock.close()
            
        except:
            pass
        
        return service_info
    
    def vulnerability_scan(self, host: str, port: int, service: str) -> List[Dict]:
        """Basic vulnerability scanning"""
        vulnerabilities = []
        
        # Check for common vulnerabilities
        if service == 'SSH' and port == 22:
            # Check for weak SSH configuration
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(3)
                sock.connect((host, port))
                banner = sock.recv(1024).decode('utf-8', errors='ignore')
                
                # Check for old SSH versions
                if 'OpenSSH_7.' in banner or 'OpenSSH_6.' in banner:
                    vulnerabilities.append({
                        'type': 'Outdated Software',
                        'severity': 'Medium',
                        'description': f'Outdated SSH version detected: {banner.strip()}',
                        'recommendation': 'Update SSH to latest version'
                    })
                
                sock.close()
            except:
                pass
        
        elif service == 'HTTP' and port in [80, 8080]:
            # Check for common web vulnerabilities
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(3)
                sock.connect((host, port))
                
                # Check for server information disclosure
                request = f'GET / HTTP/1.1\r\nHost: {host}\r\n\r\n'
                sock.send(request.encode())
                response = sock.recv(4096).decode('utf-8', errors='ignore')
                
                if 'Server:' in response:
                    vulnerabilities.append({
                        'type': 'Information Disclosure',
                        'severity': 'Low',
                        'description': 'Server version information disclosed in HTTP headers',
                        'recommendation': 'Configure server to hide version information'
                    })
                
                # Check for directory listing
                request = f'GET /admin/ HTTP/1.1\r\nHost: {host}\r\n\r\n'
                sock.send(request.encode())
                response = sock.recv(4096).decode('utf-8', errors='ignore')
                
                if 'Index of' in response:
                    vulnerabilities.append({
                        'type': 'Directory Listing',
                        'severity': 'Medium',
                        'description': 'Directory listing enabled on /admin/',
                        'recommendation': 'Disable directory listing'
                    })
                
                sock.close()
            except:
                pass
        
        return vulnerabilities
    
    def comprehensive_audit(self, network_base: str) -> Dict:
        """Perform comprehensive network audit"""
        print(f"Starting network audit for {network_base}.0/24")
        
        # Step 1: Host discovery
        print("Step 1: Discovering live hosts...")
        live_hosts = self.scan_network_range(network_base)
        
        # Step 2: Port scanning
        print("Step 2: Scanning ports...")
        for host in live_hosts:
            print(f"Scanning ports on {host}")
            open_ports = self.port_scan(host)
            
            # Step 3: Service detection
            for port in open_ports:
                service_info = self.service_detection(host, port)
                self.services[f"{host}:{port}"] = service_info
                
                # Step 4: Vulnerability scanning
                vulns = self.vulnerability_scan(host, port, service_info['service'])
                for vuln in vulns:
                    vuln['host'] = host
                    vuln['port'] = port
                    self.vulnerabilities.append(vuln)
        
        # Generate audit report
        return self.generate_audit_report()
    
    def generate_audit_report(self) -> Dict:
        """Generate comprehensive audit report"""
        report = {
            'scan_timestamp': time.strftime('%Y-%m-%d %H:%M:%S'),
            'summary': {
                'hosts_discovered': len(self.discovered_hosts),
                'total_open_ports': sum(len(ports) for ports in self.open_ports.values()),
                'vulnerabilities_found': len(self.vulnerabilities),
                'critical_vulns': len([v for v in self.vulnerabilities if v['severity'] == 'Critical']),
                'high_vulns': len([v for v in self.vulnerabilities if v['severity'] == 'High']),
                'medium_vulns': len([v for v in self.vulnerabilities if v['severity'] == 'Medium']),
                'low_vulns': len([v for v in self.vulnerabilities if v['severity'] == 'Low'])
            },
            'discovered_hosts': self.discovered_hosts,
            'open_ports': self.open_ports,
            'services': self.services,
            'vulnerabilities': self.vulnerabilities,
            'recommendations': self.generate_recommendations()
        }
        
        return report
    
    def generate_recommendations(self) -> List[str]:
        """Generate security recommendations based on findings"""
        recommendations = []
        
        # Check for common issues
        if any(21 in ports for ports in self.open_ports.values()):
            recommendations.append("FTP service detected. Consider using SFTP instead for secure file transfer.")
        
        if any(23 in ports for ports in self.open_ports.values()):
            recommendations.append("Telnet service detected. Replace with SSH for secure remote access.")
        
        if any(80 in ports for ports in self.open_ports.values()):
            recommendations.append("HTTP service detected. Consider implementing HTTPS for encrypted communication.")
        
        if len(self.vulnerabilities) > 0:
            recommendations.append("Vulnerabilities detected. Prioritize patching based on severity levels.")
        
        # General recommendations
        recommendations.extend([
            "Implement network segmentation to limit attack surface.",
            "Use firewalls to restrict unnecessary port access.",
            "Regularly update and patch all network services.",
            "Implement intrusion detection and prevention systems.",
            "Conduct regular security audits and penetration testing."
        ])
        
        return recommendations

# Usage example (for educational purposes only)
# auditor = NetworkAuditor()
# 
# # Scan local network (replace with appropriate network)
# report = auditor.comprehensive_audit("192.168.1")
# 
# print("=== NETWORK AUDIT REPORT ===")
# print(f"Hosts discovered: {report['summary']['hosts_discovered']}")
# print(f"Open ports: {report['summary']['total_open_ports']}")
# print(f"Vulnerabilities: {report['summary']['vulnerabilities_found']}")
```

### Network Configuration Audit

**Firewall Rule Analysis**:
```python
import re
from typing import List, Dict, Tuple

class FirewallAuditor:
    def __init__(self):
        self.rules = []
        self.findings = []
        self.risk_score = 0
    
    def parse_iptables_rules(self, rules_text: str) -> List[Dict]:
        """Parse iptables rules from text"""
        parsed_rules = []
        
        for line in rules_text.split('\n'):
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            
            rule = self.parse_iptables_rule(line)
            if rule:
                parsed_rules.append(rule)
        
        self.rules = parsed_rules
        return parsed_rules
    
    def parse_iptables_rule(self, rule_line: str) -> Dict:
        """Parse individual iptables rule"""
        rule = {
            'original': rule_line,
            'chain': '',
            'target': '',
            'protocol': '',
            'source': '',
            'destination': '',
            'port': '',
            'interface': ''
        }
        
        # Extract chain
        if '-A' in rule_line:
            chain_match = re.search(r'-A (\w+)', rule_line)
            if chain_match:
                rule['chain'] = chain_match.group(1)
        
        # Extract target
        if '-j' in rule_line:
            target_match = re.search(r'-j (\w+)', rule_line)
            if target_match:
                rule['target'] = target_match.group(1)
        
        # Extract protocol
        if '-p' in rule_line:
            proto_match = re.search(r'-p (\w+)', rule_line)
            if proto_match:
                rule['protocol'] = proto_match.group(1)
        
        # Extract source
        if '-s' in rule_line:
            source_match = re.search(r'-s ([\d./]+)', rule_line)
            if source_match:
                rule['source'] = source_match.group(1)
        
        # Extract destination
        if '-d' in rule_line:
            dest_match = re.search(r'-d ([\d./]+)', rule_line)
            if dest_match:
                rule['destination'] = dest_match.group(1)
        
        # Extract port
        if '--dport' in rule_line:
            port_match = re.search(r'--dport (\d+)', rule_line)
            if port_match:
                rule['port'] = port_match.group(1)
        
        return rule
    
    def audit_firewall_rules(self) -> Dict:
        """Audit firewall rules for security issues"""
        self.findings = []
        self.risk_score = 0
        
        # Check for overly permissive rules
        self.check_permissive_rules()
        
        # Check for default policies
        self.check_default_policies()
        
        # Check for logging
        self.check_logging_rules()
        
        # Check for rule order
        self.check_rule_order()
        
        # Check for unused rules
        self.check_unused_rules()
        
        return {
            'total_rules': len(self.rules),
            'findings': self.findings,
            'risk_score': self.risk_score,
            'recommendations': self.generate_firewall_recommendations()
        }
    
    def check_permissive_rules(self):
        """Check for overly permissive firewall rules"""
        for i, rule in enumerate(self.rules):
            # Check for rules allowing all traffic
            if (rule['source'] == '0.0.0.0/0' and 
                rule['target'] == 'ACCEPT' and 
                not rule['port']):
                
                self.findings.append({
                    'type': 'Overly Permissive Rule',
                    'severity': 'High',
                    'rule_number': i + 1,
                    'description': 'Rule allows all traffic from any source',
                    'rule': rule['original']
                })
                self.risk_score += 20
            
            # Check for rules allowing access to sensitive ports
            sensitive_ports = ['22', '3389', '1433', '3306', '5432']
            if (rule['port'] in sensitive_ports and 
                rule['source'] == '0.0.0.0/0' and 
                rule['target'] == 'ACCEPT'):
                
                self.findings.append({
                    'type': 'Sensitive Port Exposure',
                    'severity': 'Critical',
                    'rule_number': i + 1,
                    'description': f'Port {rule["port"]} accessible from anywhere',
                    'rule': rule['original']
                })
                self.risk_score += 30
    
    def check_default_policies(self):
        """Check default firewall policies"""
        chains = ['INPUT', 'OUTPUT', 'FORWARD']
        
        for chain in chains:
            chain_rules = [r for r in self.rules if r['chain'] == chain]
            
            if not chain_rules:
                self.findings.append({
                    'type': 'Missing Chain Rules',
                    'severity': 'Medium',
                    'description': f'No rules defined for {chain} chain',
                    'recommendation': f'Define explicit rules for {chain} chain'
                })
                self.risk_score += 10
    
    def check_logging_rules(self):
        """Check for logging rules"""
        log_rules = [r for r in self.rules if 'LOG' in r['target']]
        
        if not log_rules:
            self.findings.append({
                'type': 'No Logging Rules',
                'severity': 'Medium',
                'description': 'No logging rules found for security monitoring',
                'recommendation': 'Add logging rules for denied traffic'
            })
            self.risk_score += 15
    
    def check_rule_order(self):
        """Check firewall rule order for conflicts"""
        for i in range(len(self.rules) - 1):
            current_rule = self.rules[i]
            
            # Check if a more specific rule comes after a general rule
            for j in range(i + 1, len(self.rules)):
                next_rule = self.rules[j]
                
                if (current_rule['chain'] == next_rule['chain'] and
                    current_rule['source'] == '0.0.0.0/0' and
                    next_rule['source'] != '0.0.0.0/0' and
                    current_rule['target'] == 'ACCEPT'):
                    
                    self.findings.append({
                        'type': 'Rule Order Issue',
                        'severity': 'Medium',
                        'description': f'Specific rule at position {j+1} may be unreachable due to general rule at position {i+1}',
                        'affected_rules': [current_rule['original'], next_rule['original']]
                    })
                    self.risk_score += 10
                    break
    
    def check_unused_rules(self):
        """Check for potentially unused rules"""
        # This is a simplified check - in practice, you'd analyze traffic logs
        rule_usage = {}
        
        for i, rule in enumerate(self.rules):
            # Simulate rule usage analysis
            if rule['target'] == 'DROP' and rule['source'] == '0.0.0.0/0':
                rule_usage[i] = 0  # Assume unused for demo
        
        for rule_index, usage_count in rule_usage.items():
            if usage_count == 0:
                self.findings.append({
                    'type': 'Potentially Unused Rule',
                    'severity': 'Low',
                    'rule_number': rule_index + 1,
                    'description': 'Rule may be unused based on traffic analysis',
                    'rule': self.rules[rule_index]['original']
                })
    
    def generate_firewall_recommendations(self) -> List[str]:
        """Generate firewall security recommendations"""
        recommendations = [
            "Implement principle of least privilege - only allow necessary traffic",
            "Use specific source and destination addresses instead of 0.0.0.0/0 when possible",
            "Add logging rules for security monitoring and incident response",
            "Regularly review and update firewall rules",
            "Implement rule documentation and change management processes",
            "Use network segmentation to limit lateral movement",
            "Consider implementing application-layer filtering",
            "Regularly test firewall rules with security assessments"
        ]
        
        return recommendations

# Example usage
sample_iptables_rules = """
-A INPUT -p tcp --dport 22 -s 0.0.0.0/0 -j ACCEPT
-A INPUT -p tcp --dport 80 -j ACCEPT
-A INPUT -p tcp --dport 443 -j ACCEPT
-A INPUT -p tcp --dport 3306 -s 0.0.0.0/0 -j ACCEPT
-A INPUT -j DROP
"""

firewall_auditor = FirewallAuditor()
firewall_auditor.parse_iptables_rules(sample_iptables_rules)
audit_result = firewall_auditor.audit_firewall_rules()

print("=== FIREWALL AUDIT RESULTS ===")
print(f"Total rules: {audit_result['total_rules']}")
print(f"Risk score: {audit_result['risk_score']}")
print(f"Findings: {len(audit_result['findings'])}")

for finding in audit_result['findings']:
    print(f"\n{finding['type']} ({finding['severity']}):")
    print(f"  {finding['description']}")
```

---

*Continue with Systems Audit, Advanced Security Concepts, and MCQ Questions in the next response due to length constraints*