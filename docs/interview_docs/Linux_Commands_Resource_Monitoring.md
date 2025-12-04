# Linux Commands - Resource Monitoring & Process Management

## 1. Finding Processes Consuming Maximum Resources

### CPU Usage

```bash
# Top 10 processes by CPU usage
ps aux --sort=-%cpu | head -11

# Real-time monitoring (interactive)
top

# Press 'P' to sort by CPU
# Press 'q' to quit

# Top 10 with htop (better visualization)
htop
# Press F6 -> Select %CPU -> Enter

# One-liner: Top 5 CPU consumers
ps aux --sort=-%cpu | awk 'NR<=6{printf "%-10s %-8s %-8s %s\n", $2, $3, $4, $11}'
```

**Output**:
```
PID        %CPU     %MEM     COMMAND
12345      45.2     12.3     java
67890      23.1     8.5      mysql
11223      15.7     5.2      node
```

---

### Memory Usage

```bash
# Top 10 processes by memory usage
ps aux --sort=-%mem | head -11

# Top 5 memory consumers
ps aux --sort=-%mem | awk 'NR<=6{printf "%-10s %-8s %-8s %s\n", $2, $3, $4, $11}'

# Memory usage in MB
ps aux --sort=-%mem | awk 'NR<=11{printf "%-10s %-8s %-10s %s\n", $2, $3, $6/1024"MB", $11}'
```

---

### Combined CPU + Memory

```bash
# Top processes by CPU and Memory
top -b -n 1 | head -20

# Custom format
ps -eo pid,ppid,%cpu,%mem,vsz,rss,comm --sort=-%cpu | head -11
```

**Output**:
```
PID    PPID  %CPU %MEM    VSZ   RSS COMMAND
12345  1     45.2 12.3 4567890 123456 java
67890  1     23.1  8.5 2345678  87654 mysql
```

---

### Disk I/O Usage

```bash
# Install iotop if not available
sudo apt-get install iotop  # Ubuntu/Debian
sudo yum install iotop      # CentOS/RHEL

# Monitor disk I/O
sudo iotop

# Top 10 processes by I/O
sudo iotop -b -n 1 | head -20
```

---

### Network Usage

```bash
# Install nethogs if not available
sudo apt-get install nethogs

# Monitor network usage by process
sudo nethogs

# Alternative: iftop
sudo iftop
```

---

## 2. Finding Process ID Running on Specific Port

### Method 1: lsof (List Open Files)

```bash
# Find process on port 8080
lsof -i :8080

# Get only PID
lsof -ti :8080

# Detailed info
lsof -i :8080 -P -n
```

**Output**:
```
COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
java    12345   user   45u  IPv6 123456      0t0  TCP *:8080 (LISTEN)
```

---

### Method 2: netstat

```bash
# Find process on port 8080
netstat -tulpn | grep :8080

# Only listening ports
netstat -tlnp | grep :8080

# Get PID only
netstat -tlnp | grep :8080 | awk '{print $7}' | cut -d'/' -f1
```

**Output**:
```
tcp6  0  0 :::8080  :::*  LISTEN  12345/java
```

---

### Method 3: ss (Socket Statistics)

```bash
# Find process on port 8080
ss -tulpn | grep :8080

# Get PID
ss -tulpn | grep :8080 | awk '{print $7}' | grep -oP '\d+(?=/)'
```

**Output**:
```
tcp   LISTEN  0  128  *:8080  *:*  users:(("java",pid=12345,fd=45))
```

---

### Method 4: fuser

```bash
# Find process on port 8080
fuser 8080/tcp

# Kill process on port 8080
fuser -k 8080/tcp

# Verbose output
fuser -v 8080/tcp
```

**Output**:
```
8080/tcp:  12345
```

---

### Kill Process on Specific Port

```bash
# Method 1: Using lsof
kill -9 $(lsof -ti :8080)

# Method 2: Using fuser
fuser -k 8080/tcp

# Method 3: Using netstat
kill -9 $(netstat -tlnp | grep :8080 | awk '{print $7}' | cut -d'/' -f1)

# Method 4: Using ss
kill -9 $(ss -tulpn | grep :8080 | awk '{print $7}' | grep -oP '\d+(?=/)')
```

---

## 3. Finding Older Files

### Files Modified More Than N Days Ago

```bash
# Files modified more than 30 days ago
find /path/to/directory -type f -mtime +30

# Files modified more than 7 days ago
find /var/log -type f -mtime +7

# Files modified exactly 30 days ago
find /path -type f -mtime 30

# Files modified less than 7 days ago
find /path -type f -mtime -7
```

---

### Files Accessed More Than N Days Ago

```bash
# Files accessed more than 30 days ago
find /path -type f -atime +30

# Files not accessed in last 90 days
find /home/user -type f -atime +90
```

---

### Files Created More Than N Days Ago

```bash
# Files created more than 30 days ago (creation time)
find /path -type f -ctime +30
```

---

### Files Older Than Specific Date

```bash
# Files modified before 2024-01-01
find /path -type f -newermt 2024-01-01 ! -newermt 2024-01-02

# Files older than specific date
find /path -type f ! -newermt "2024-01-01"

# Files modified in date range
find /path -type f -newermt "2024-01-01" ! -newermt "2024-02-01"
```

---

### Delete Old Files

```bash
# Delete files older than 30 days
find /path -type f -mtime +30 -delete

# Delete with confirmation
find /path -type f -mtime +30 -exec rm -i {} \;

# Delete and show deleted files
find /path -type f -mtime +30 -exec rm -v {} \;
```

---

### Find and List Old Files with Details

```bash
# List files older than 30 days with size
find /path -type f -mtime +30 -exec ls -lh {} \;

# List with human-readable format
find /path -type f -mtime +30 -printf "%TY-%Tm-%Td %TH:%TM %p %s bytes\n"

# Sort by modification time
find /path -type f -mtime +30 -printf "%T@ %Tc %p\n" | sort -n
```

---

### Find Large Old Files

```bash
# Files older than 30 days and larger than 100MB
find /path -type f -mtime +30 -size +100M

# Files older than 30 days and larger than 1GB
find /path -type f -mtime +30 -size +1G -exec ls -lh {} \;

# Top 10 largest old files
find /path -type f -mtime +30 -exec du -h {} \; | sort -rh | head -10
```

---

### Archive Old Files

```bash
# Archive files older than 30 days
find /path -type f -mtime +30 -print0 | tar -czvf old_files_$(date +%Y%m%d).tar.gz --null -T -

# Move old files to archive directory
find /path -type f -mtime +30 -exec mv {} /archive/ \;
```

---

### Find Old Log Files

```bash
# Log files older than 7 days
find /var/log -type f -name "*.log" -mtime +7

# Compressed log files older than 30 days
find /var/log -type f -name "*.gz" -mtime +30

# Delete old log files
find /var/log -type f -name "*.log" -mtime +30 -delete
```

---

## Quick Reference Cheat Sheet

### Resource Monitoring
```bash
# CPU top 5
ps aux --sort=-%cpu | head -6

# Memory top 5
ps aux --sort=-%mem | head -6

# Real-time monitoring
top
htop
```

### Port to PID
```bash
# Quick methods
lsof -ti :8080
fuser 8080/tcp
netstat -tlnp | grep :8080
ss -tulpn | grep :8080
```

### Old Files
```bash
# Modified > 30 days
find /path -type f -mtime +30

# Accessed > 30 days
find /path -type f -atime +30

# Size > 100MB and > 30 days old
find /path -type f -mtime +30 -size +100M

# Delete old files
find /path -type f -mtime +30 -delete
```

---

## Time Options Explained

| Option | Meaning | Example |
|--------|---------|---------|
| `-mtime +30` | Modified MORE than 30 days ago | Files older than 30 days |
| `-mtime -30` | Modified LESS than 30 days ago | Files newer than 30 days |
| `-mtime 30` | Modified EXACTLY 30 days ago | Files from exactly 30 days |
| `-atime +30` | Accessed MORE than 30 days ago | Not accessed in 30+ days |
| `-ctime +30` | Status changed MORE than 30 days ago | Metadata changed 30+ days |

---

## Size Options

| Option | Meaning |
|--------|---------|
| `-size +100M` | Larger than 100 MB |
| `-size -100M` | Smaller than 100 MB |
| `-size +1G` | Larger than 1 GB |
| `-size +100k` | Larger than 100 KB |
