/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker.tasks.container

import com.bmuschko.gradle.docker.tasks.image.DockerExistingImage
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Device
import com.github.dockerjava.api.model.InternetProtocol
import com.github.dockerjava.api.model.Link
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.RestartPolicy
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.api.model.VolumesFrom
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

class DockerCreateContainer extends DockerExistingImage {
    @Input
    @Optional
    final Property<String> containerName = project.objects.property(String)

    @Input
    @Optional
    final Property<String> hostName = project.objects.property(String)

    @Input
    @Optional
    final Property<String> ipv4Address = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> portSpecs = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> user = project.objects.property(String)

    @Input
    @Optional
    final Property<Boolean> stdinOpen = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> stdinOnce = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStdin = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStdout = project.objects.property(Boolean)

    @Input
    @Optional
    final Property<Boolean> attachStderr = project.objects.property(Boolean)

    @Input
    @Optional
    final MapProperty<String, String> envVars = project.objects.mapProperty(String, String)

    @Input
    @Optional
    final ListProperty<String> cmd = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> entrypoint = project.objects.listProperty(String)


    @Input
    @Optional
    final ListProperty<String> networkAliases = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> image = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> volumes = project.objects.listProperty(String)

    @Input
    @Optional
    final Property<String> workingDir = project.objects.property(String)

    @Input
    final ListProperty<ExposedPort> exposedPorts = project.objects.listProperty(ExposedPort)

    @Input
    @Optional
    final Property<Boolean> tty = project.objects.property(Boolean)

    @Input
    @Optional
    final MapProperty<String, String> labels = project.objects.mapProperty(String, String)

    @Internal
    final Property<String> containerId = project.objects.property(String)

    @Input
    @Optional
    final Property<String> macAddress = project.objects.property(String)
    
    final Property<HostConfig> hostConfig = project.objects.property(HostConfig)
    
    DockerCreateContainer() {
        portSpecs.empty()
        stdinOpen.set(false)
        stdinOnce.set(false)
        attachStdin.set(false)
        attachStdout.set(false)
        attachStderr.set(false)
        cmd.empty()
        entrypoint.empty()
        networkAliases.empty()
        volumes.empty()
        exposedPorts.empty()
        tty.set(false)
    }

    @Override
    void runRemoteCommand() {
        CreateContainerCmd containerCommand = dockerClient.createContainerCmd(imageId.get())
        setContainerCommandConfig(containerCommand)
        CreateContainerResponse container = containerCommand.exec()
        final String localContainerName = containerName.getOrNull() ?: container.id
        logger.quiet "Created container with ID '$localContainerName'."
        containerId.set(container.id)
        if(nextHandler) {
            nextHandler.execute(container)
        }
    }
    
    @Nested
    public Property<HostConfig> getHostConfig() {
        return hostConfig;
    }

    void exposePorts(String internetProtocol, List<Integer> ports) {
        exposedPorts.add(new ExposedPort(internetProtocol, ports))
    }

    void withEnvVar(def key, def value) {
        if (envVars.getOrNull()) {
            envVars.put(key, value)
        } else {
            envVars.set([(key): value])
        }
    }

    private void setContainerCommandConfig(CreateContainerCmd containerCommand) {
        if(containerName.getOrNull()) {
            containerCommand.withName(containerName.get())
        }

        if(hostName.getOrNull()) {
            containerCommand.withHostName(hostName.get())
        }

        if(ipv4Address.getOrNull()){
            containerCommand.withIpv4Address(ipv4Address.get())
        }

        if(portSpecs.getOrNull()) {
            containerCommand.withPortSpecs(portSpecs.get())
        }

        if(user.getOrNull()) {
            containerCommand.withUser(user.get())
        }

        if(stdinOpen.getOrNull()) {
            containerCommand.withStdinOpen(stdinOpen.get())
        }

        if(stdinOnce.getOrNull()) {
            containerCommand.withStdInOnce(stdinOnce.get())
        }

        if(attachStdin.getOrNull()) {
            containerCommand.withAttachStdin(attachStdin.get())
        }

        if(attachStdout.getOrNull()) {
            containerCommand.withAttachStdout(attachStdout.get())
        }

        if(attachStderr.getOrNull()) {
            containerCommand.withAttachStderr(attachStderr.get())
        }

        // marshall map into list
        if(envVars.getOrNull()) {
            containerCommand.withEnv(envVars.get().collect { key, value -> "${key}=${value}".toString() })
        }

        if(cmd.getOrNull()) {
            containerCommand.withCmd(cmd.get())
        }

        if(entrypoint.getOrNull()) {
            containerCommand.withEntrypoint(entrypoint.get())
        }

        if(networkAliases.getOrNull()) {
            containerCommand.withAliases(networkAliases.get())
        }

        if(image.getOrNull()) {
            containerCommand.withImage(image.get())
        }

        if(volumes.getOrNull()) {
            List<Volume> createdVolumes = volumes.get().collect { Volume.parse(it) }
            containerCommand.withVolumes(createdVolumes)
        }

        if(workingDir.getOrNull()) {
            containerCommand.withWorkingDir(workingDir.get())
        }

        if(exposedPorts.getOrNull()) {
            List<List<com.github.dockerjava.api.model.ExposedPort>> allPorts = exposedPorts.get().collect { exposedPort ->
                exposedPort.ports.collect {
                    Integer port -> new com.github.dockerjava.api.model.ExposedPort(port, InternetProtocol.parse(exposedPort.internetProtocol.toLowerCase()))
                }
            }
            containerCommand.withExposedPorts(allPorts.flatten() as List<com.github.dockerjava.api.model.ExposedPort>)
        }

        if(tty.getOrNull()) {
            containerCommand.withTty(tty.get())
        }


        if(labels.getOrNull()) {
            containerCommand.withLabels(labels.get().collectEntries { [it.key, it.value.toString()] })
        }

        if(macAddress.getOrNull()) {
            containerCommand.withMacAddress(macAddress.get())
        }
        
        if(hostConfig.getOrNull()) {
          HostConfig hc = hostConfig.get();
          hc.setCreateContainerCommandHostConfig(cmd);
        }
        
    }

    static class LogConfig {
        @Input String type
        @Input Map<String, String> config = [:]
    }

    static class ExposedPort {
        @Input final String internetProtocol
        @Input final List<Integer> ports = []

        ExposedPort(String internetProtocol, List<Integer> ports) {
            this.internetProtocol = internetProtocol
            this.ports = ports
        }
    }
    
    class HostConfig {
      //alphabetical order
      
      /**
       * Automatically remove the container when the container's process exits.
       *
       * This has no effect if {@link #restartPolicy} is set.
       * @since 3.6.2
       */
      @Input
      @Optional
      final Property<Boolean> autoRemove = project.objects.property(Boolean)
      
      @Input
      @Optional
      final MapProperty<String, String> binds = project.objects.mapProperty(String, String)
      
      /**
       * CPUs in which to allow execution (0-3, 0,1)
       */
      @Input
      @Optional
      final Property<String> cpuset = project.objects.property(String)
      
      /**
       *  Add host devices to the container
       */
      @Input
      @Optional
      final ListProperty<String> devices = project.objects.listProperty(String)
      
      /**
       * Set custom DNS servers
       */
      @Input
      @Optional
      final ListProperty<String> dns = project.objects.listProperty(String)
  
      /**
       * Add custom host-to-IP mappings (host:ip)
       */
      @Input
      @Optional
      final ListProperty<String> extraHosts = project.objects.listProperty(String)
      
      /**
       * A list of additional groups that the container process will run as.
       *
       * @since 4.4.0
       */
      @Input
      @Optional
      final ListProperty<String> groups = project.objects.listProperty(String)
      
      /**
       * Set the IPC mode for the container
       * "none"- Own private IPC namespace, with /dev/shm not mounted.
       * "private" -  Own private IPC namespace.
       * "shareable" - Own private IPC namespace, with a possibility to share it with other containers.
       * "container: <_name-or-ID_>" - Join another ("shareable") container’s IPC namespace.
       * "host" - Use the host system’s IPC namespace.
       * @since 5.2
       */
      @Input
      @Optional
      final Property<String> ipcMode = project.objects.property(String)
      
      /**
       * Adds links to another container
       */
      @Input
      @Optional
      final ListProperty<String> links = project.objects.listProperty(String)
      
      @Input
      @Optional
      final Property<LogConfig> logConfig = project.objects.property(LogConfig)
      
      /**
       * Memory limit
       */
      @Input
      @Optional
      final Property<Long> memory = project.objects.property(Long)

      /**
       * Swap limit equal to memory plus swap: ‘-1’ to enable unlimited swap
       */
      @Input
      @Optional
      final Property<Long> memorySwap = project.objects.property(Long)
      
      /**
       * Connect a container to a network
       */
      @Input
      @Optional
      final Property<String> network = project.objects.property(String)
      
      
      /**
       * PID namespace to use
       */
      @Input
      @Optional
      final Property<String> pid = project.objects.property(String)
      
      @Input
      @Optional
      final ListProperty<String> portBindings = project.objects.listProperty(String)
      
      /**
       * Publish all exposed ports to random ports
       */
      @Input
      @Optional
      final Property<Boolean> publishAll = project.objects.property(Boolean)
      
      /**
       * Give extended privileges to this container
       */
      @Input
      @Optional
      final Property<Boolean> privileged = project.objects.property(Boolean)
      
      /**
       * Restart policy to apply when a container exits
       */
      @Input
      @Optional
      final Property<String> restartPolicy = project.objects.property(String)
      
      /**
       * Size of <code>/dev/shm</code> in bytes.
       * The size must be greater than 0.
       * If omitted the system uses 64MB.
       */
      @Input
      @Optional
      final Property<Long> shmSize = project.objects.property(Long)
            
      /**
       * Sets namespaced kernel parameters (sysctls) in the container. 
       * For example, to turn on IP forwarding in the containers network namespace:
       * sysctls = ['net.ipv4.ip_forward':'1']
       * <strong>Note:</strong> Not all sysctls are namespaced. 
       * Docker does not support changing sysctls inside of a container that also modify the host system. 
       * @since 5.2
       */
      @Input
      @Optional
      final MapProperty<String, String> sysctls = project.objects.mapProperty(String, String)
      
      /**
       * Mount volumes from the specified container(s)
       */
      @Input
      @Optional
      final ListProperty<String> volumesFrom = project.objects.listProperty(String)
      
      HostConfig() {
        //alphabetical order
        
        autoRemove.set(false)
        devices.empty()
        dns.empty()
        extraHosts.empty()
        groups.empty()
        links.empty()
        portBindings.empty()
        privileged.set(false)
        publishAll.set(false)
        volumesFrom.empty()
        
      }
      
      public setCreateContainerCommandHostConfig(CreateContainerCmd containerCommand) {
        //alphabetical order
        
        if(autoRemove.getOrNull()) {
            containerCommand.hostConfig.withAutoRemove(autoRemove.get())
        }
        
        if(binds.getOrNull()) {
            List<Bind> createdBinds = binds.get().collect { Bind.parse([it.key, it.value].join(':')) }
            containerCommand.hostConfig.withBinds(createdBinds)
        }

        if(cpuset.getOrNull()) {
            containerCommand.hostConfig.withCpusetCpus(cpuset.get())
        }
        
        if(devices.getOrNull()) {
          List<Device> createdDevices = devices.get().collect { Device.parse(it) }
          containerCommand.hostConfig.withDevices(createdDevices)
        }
        
        if(dns.getOrNull()) {
          containerCommand.hostConfig.withDns(dns.get())
        }
        
        if(extraHosts.getOrNull()) {
            containerCommand.hostConfig.withExtraHosts(extraHosts.get() as String[])
        }
        
        if(groups.getOrNull()) {
          containerCommand.hostConfig.withGroupAdd(groups.get())
        }
        
        if(ipcMode.getOrNull()) {
          containerCommand.hostConfig.withIpcMode(ipcMode.get())
        }
        
        if(links.getOrNull()) {
          List<Link> createdLinks = links.get().collect { Link.parse(it) }
          containerCommand.hostConfig.withLinks(createdLinks as Link[])
        }
        
        if(logConfig.getOrNull()) {
          com.github.dockerjava.api.model.LogConfig.LoggingType type = com.github.dockerjava.api.model.LogConfig.LoggingType.fromValue(logConfig.get().type)
          com.github.dockerjava.api.model.LogConfig config = new com.github.dockerjava.api.model.LogConfig(type, logConfig.get().config)
          containerCommand.hostConfig.withLogConfig(config)
        }
        
        if(memory.getOrNull()) {
          containerCommand.hostConfig.withMemory(memory.get())
        }
  
        if(memorySwap.getOrNull()) {
            containerCommand.hostConfig.withMemorySwap(memorySwap.get())
        }
  
        if(network.getOrNull()) {
            containerCommand.hostConfig.withNetworkMode(network.get())
        }
                
        if(pid.getOrNull()) {
          containerCommand.getHostConfig().withPidMode(pid.get())
        }
      
        if(portBindings.getOrNull()) {
            List<PortBinding> createdPortBindings = portBindings.get().collect { PortBinding.parse(it) }
            containerCommand.hostConfig.withPortBindings(new Ports(createdPortBindings as PortBinding[]))
        }
        
        if(privileged.getOrNull()) {
          containerCommand.hostConfig.withPrivileged(privileged.get())
        }

        if(publishAll.getOrNull()) {
            containerCommand.hostConfig.withPublishAllPorts(publishAll.get())
        }
        
        if(restartPolicy.getOrNull()) {
          containerCommand.hostConfig.withRestartPolicy(RestartPolicy.parse(restartPolicy.get()))
        }
        
        if(shmSize.getOrNull() != null) { // 0 is valid input
          containerCommand.hostConfig.withShmSize(shmSize.get())
        }
        
        
        if(sysctls.getOrNull()) {
            containerCommand.hostConfig.withSysctls(sysctls.get())
        }
        
        if(volumesFrom.getOrNull()) {
          List<VolumesFrom> createdVolumes = volumesFrom.get().collect { new VolumesFrom(it) }
          containerCommand.hostConfig.withVolumesFrom(createdVolumes)
        }

      }
      
      void logConfig(String type, Map<String, String> config) {
        this.logConfig.set(new LogConfig(type: type, config: config))
      }
      
      void restartPolicy(String name, int maximumRetryCount) {
          this.restartPolicy.set("${name}:${maximumRetryCount}".toString())
      }
    }
}

