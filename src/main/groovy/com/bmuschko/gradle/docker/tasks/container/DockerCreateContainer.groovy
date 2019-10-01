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

import com.bmuschko.gradle.docker.tasks.image.*
import com.github.dockerjava.api.command.*
import com.github.dockerjava.api.model.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
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
    
    /**
     * Set the IPC mode for the container
     * "none"- Own private IPC namespace, with /dev/shm not mounted.
     * "private" - 	Own private IPC namespace.
     * "shareable" - Own private IPC namespace, with a possibility to share it with other containers.
     * "container: <_name-or-ID_>" - Join another ("shareable") container’s IPC namespace.
     * "host" - Use the host system’s IPC namespace.
     */
    @Input
    @Optional
    final Property<String> ipcMode = project.objects.property(String)
    
    @Input
    @Optional
    final MapProperty<String, String> sysctls = project.objects.mapProperty(String, String)
    
    @Nested
    @Optional
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
    
    void logConfig(String type, Map<String, String> config) {
        this.hostConfig.get().logConfig = new LogConfig(type: type, config: config)
    }

    void exposePorts(String internetProtocol, List<Integer> ports) {
        exposedPorts.add(new ExposedPort(internetProtocol, ports))
    }

    void restartPolicy(String name, int maximumRetryCount) {
        this.hostConfig.get().restartPolicy = "${name}:${maximumRetryCount}"
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
        
        if(ipcMode.getOrNull()) {
            containerCommand.hostConfig.withIpcMode(ipcMode.get())
        }
        
        if(sysctls.getOrNull()) {
            containerCommand.hostConfig.withSysctls(sysctls.get())
        }
        
        if(hostConfig.getOrNull()) {
            HostConfig config = hostConfig.get()
            
            if(config.groups) {
                containerCommand.hostConfig.withGroupAdd(config.groups)
            }
            
            if(config.memory) {
                containerCommand.hostConfig.withMemory(config.memory)
            }
    
            if(config.memorySwap) {
                containerCommand.hostConfig.withMemorySwap(config.memorySwap)
            }
    
            if(config.cpuset) {
                containerCommand.hostConfig.withCpusetCpus(config.cpuset)
            }
            
            if(config.dns) {
              containerCommand.hostConfig.withDns(config.dns)
            }
  
            if(config.network) {
                containerCommand.hostConfig.withNetworkMode(config.network)
            }
            
            if (config.links) {
                List<Link> createdLinks = config.links.collect { Link.parse(it) }
                containerCommand.hostConfig.withLinks(createdLinks as Link[])
            }
  
            if(config.volumesFrom) {
                List<VolumesFrom> createdVolumes = config.volumesFrom.collect { new VolumesFrom(it) }
                containerCommand.hostConfig.withVolumesFrom(createdVolumes)
            }
            
            if(config.portBindings) {
              List<PortBinding> createdPortBindings = config.portBindings.collect { PortBinding.parse(it) }
              containerCommand.hostConfig.withPortBindings(new Ports(createdPortBindings as PortBinding[]))
            }
  
            if(config.publishAll) {
                containerCommand.hostConfig.withPublishAllPorts(config.publishAll)
            }
    
            if(config.binds) {
                List<Bind> createdBinds = config.binds.collect { Bind.parse([it.key, it.value].join(':')) }
                containerCommand.hostConfig.withBinds(createdBinds)
            }
    
            if(config.extraHosts) {
                containerCommand.hostConfig.withExtraHosts(config.extraHosts.toArray())
            }
    
            if(config.logConfig) {
                com.github.dockerjava.api.model.LogConfig.LoggingType type = com.github.dockerjava.api.model.LogConfig.LoggingType.fromValue(config.logConfig.type)
                com.github.dockerjava.api.model.LogConfig logConfig = new com.github.dockerjava.api.model.LogConfig(type, config.logConfig.config)
                containerCommand.hostConfig.withLogConfig(logConfig)
            }
    
            if(config.privileged) {
                containerCommand.hostConfig.withPrivileged(config.privileged)
            }
    
            if (config.restartPolicy) {
                containerCommand.hostConfig.withRestartPolicy(RestartPolicy.parse(config.restartPolicy))
            }
    
            if (config.pid) {
                containerCommand.getHostConfig().withPidMode(config.pid)
            }
    
            if (config.devices) {
                List<Device> createdDevices = config.devices.collect { Device.parse(it) }
                containerCommand.hostConfig.withDevices(createdDevices)
            }
            if(config.shmSize != null) { // 0 is valid input
                containerCommand.hostConfig.withShmSize(config.shmSize)
            }
    
            if (config.autoRemove) {
                containerCommand.hostConfig.withAutoRemove(config.autoRemove)
            }
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
    
    static class HostConfig {
      /**
       * A list of additional groups that the container process will run as.
       *
       * @since 4.4.0
       */
      @Input
      @Optional
      final List<String> groups = []
      
      @Input
      @Optional
      final Long memory
  
      @Input
      @Optional
      final Long memorySwap
  
      @Input
      @Optional
      final String cpuset
      
      @Input
      @Optional
      final List<String> dns = []
  
      @Input
      @Optional
      final String network
      
      @Input
      @Optional
      final List<String> links = []
      
      @Input
      @Optional
      final List<String> volumesFrom = []
      
      @Input
      @Optional
      final List<String> portBindings
  
      @Input
      @Optional
      final Boolean publishAll
      
      @Input
      @Optional
      final Map<String, String> binds
  
      @Input
      @Optional
      final List<String> extraHosts = []
      
      @Input
      @Optional
      final LogConfig logConfig
  
      @Input
      @Optional
      final Boolean privileged
      
      @Input
      @Optional
      final String restartPolicy 
  
      @Input
      @Optional
      final String pid
  
      @Input
      @Optional
      final List<String> devices = []
      
      
      /**
       * Size of <code>/dev/shm</code> in bytes.
       * The size must be greater than 0.
       * If omitted the system uses 64MB.
       */
      @Input
      @Optional
      final Long shmSize
  
      /**
       * Automatically remove the container when the container's process exits.
       *
       * This has no effect if {@link #restartPolicy} is set.
       * @since 3.6.2
       */
      @Input
      @Optional
      final Boolean autoRemove
    }
}

