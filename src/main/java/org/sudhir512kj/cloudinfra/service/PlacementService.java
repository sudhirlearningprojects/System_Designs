package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.cloudinfra.model.Host;
import org.sudhir512kj.cloudinfra.repository.HostRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementService {
    private final HostRepository hostRepository;
    
    public Host selectHost(int requiredCpu, int requiredMemoryGb, String region) {
        List<Host> availableHosts = hostRepository.findByRegionAndStatus(region, Host.HostStatus.ACTIVE);
        
        // Best-fit bin packing algorithm
        return availableHosts.stream()
            .filter(h -> h.getAvailableCpu() >= requiredCpu && h.getAvailableMemoryGb() >= requiredMemoryGb)
            .min(Comparator.comparingInt(h -> h.getAvailableCpu() - requiredCpu))
            .orElseThrow(() -> new RuntimeException("No suitable host found"));
    }
    
    public void allocateResources(String hostId, int cpu, int memoryGb) {
        Host host = hostRepository.findById(hostId)
            .orElseThrow(() -> new RuntimeException("Host not found"));
        
        host.setAvailableCpu(host.getAvailableCpu() - cpu);
        host.setAvailableMemoryGb(host.getAvailableMemoryGb() - memoryGb);
        hostRepository.save(host);
    }
    
    public void releaseResources(String hostId, int cpu, int memoryGb) {
        Host host = hostRepository.findById(hostId)
            .orElseThrow(() -> new RuntimeException("Host not found"));
        
        host.setAvailableCpu(Math.min(host.getTotalCpu(), host.getAvailableCpu() + cpu));
        host.setAvailableMemoryGb(Math.min(host.getTotalMemoryGb(), host.getAvailableMemoryGb() + memoryGb));
        hostRepository.save(host);
    }
}
