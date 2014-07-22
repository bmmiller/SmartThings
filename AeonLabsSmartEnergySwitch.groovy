/**
 *  Aeon Labs Smart Energy Switch
 *
 *  Copyright 2014 Brandon Miller
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Aeon Labs Smart Energy Switch", namespace: "bmmiller", author: "Brandon Miller") {
		capability "Energy Meter"
		capability "Refresh"
		capability "Power Meter"
		capability "Switch"
		capability "Configuration"
		capability "Polling"
        capability "Sensor"
        
        attribute "power", "string"
		attribute "energy", "string"
		attribute "switch", "string"

		command "on"
		command "off"
        command "reset"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		valueTile("power", "device.power", decoration: "flat") {
			state "default", label:'${currentValue} W'
		}
		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh'
		}
		standardTile("switch", "device.switch",canChangeIcon: true) {
                        state "on", label: "switch", action: "off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
                        state "off", label: "switch", action: "on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
                }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
                        state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
                }
        standardTile("reset", "device.switch", inactiveLabel: false, decoration: "flat") {
        				state "default", label:"reset kWh", action:"reset"
                }
        standardTile("configure", "device.switch", inactiveLabel: false, decoration: "flat") {
        				state "default", label:"", action:"configure", icon:"st.secondary.configure"
                }

        main(["switch", "power", "energy"])
        details(["switch", "power", "energy", "refresh", "configure", "reset"])
	}
}
// 0x25 0x32 0x27 0x70 0x85 0x72 0x86 0x60 0xEF 0x82

// 0x25: switch binary
// 0x32: meter
// 0x27: switch all
// 0x70: configuration
// 0x85: association
// 0x86: version
// 0x60: multi-channel
// 0xEF: mark
// 0x82: hail

// parse events into attributes
def parse(String description) {
	log.debug "Parsing desc => '${description}'"

    def result = null
    def cmd = zwave.parse(description, [0x60:3, 0x25:1, 0x32:1, 0x70:1])
    if (cmd) {
        result = createEvent(zwaveEvent(cmd))
    }
    log.debug "Parsing result => '${result}'"
    return result
}

//Reports

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
        [name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
        [name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	def map = []

	if (cmd.scale == 0) {
    	map = [ name: "energy", value: cmd.scaledMeterValue, unit: "kWh" ]
    }
    else if (cmd.scale == 2) {
    	map = [ name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W" ]
    }

    map
}

def zwaveEvent(int endPoint, physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	// MeterReport(deltaTime: 1368, meterType: 1, meterValue: [0, 3, 29, 17], precision: 3, previousMeterValue: [0, 3, 29, 17], rateType: 1, reserved02: false, scale: 0, scaledMeterValue: 204.049, scaledPreviousMeterValue: 204.049, size: 4)
	 log.debug "EndPoint $endPoint, MeterReport $cmd"
    def map = []

    if (cmd.scale == 0) {
    	map = [ name: "energy" + endPoint, value: cmd.scaledMeterValue, unit: "kWh" ]
    }
    else if (cmd.scale == 2) {
    	map = [ name: "power" + endPoint, value: Math.round(cmd.scaledMeterValue), unit: "W" ]
    }

    map
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    log.debug "MultiChannelCmdEncap $cmd"

    def map = [ name: "switch$cmd.sourceEndPoint" ]
    if (cmd.commandClass == 37){
    	if (cmd.parameter == [0]) {
        	map.value = "off"
        }
        if (cmd.parameter == [255]) {
            map.value = "on"
        }
        map
    }
    else if (cmd.commandClass == 50) {
        def hex1 = { n -> String.format("%02X", n) }
        def desc = "command: ${hex1(cmd.commandClass)}${hex1(cmd.command)}, payload: " + cmd.parameter.collect{hex1(it)}.join(" ")
        zwaveEvent(cmd.sourceEndPoint, zwave.parse(desc, [ 0x60:3, 0x25:1, 0x32:1, 0x70:1 ]))
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) {
//	  [50, 37, 32], dynamic: false, endPoint: 1, genericDeviceClass: 16, specificDeviceClass: 1)
    log.debug "multichannelv3.MultiChannelCapabilityReport $cmd"
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "Configuration Report for parameter ${cmd.parameterNumber}: Value is ${cmd.configurationValue}, Size is ${cmd.size}"
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
        // Handles all Z-Wave commands we aren't interested in
        [:]
    log.debug "Capture All $cmd"
}

// handle commands
def refresh() {
	def cmds = []

 	for ( i in 1..3 )
    	cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:i, commandClass:37, command:2).format()

    for ( i in 1..3 ) {
        cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:i, commandClass:50, command:1, parameter:[0]).format()
        cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:i, commandClass:50, command:1, parameter:[16]).format()
	}

    cmds << zwave.meterV2.meterGet(scale:0).format()
    cmds << zwave.meterV2.meterGet(scale:2).format()

    delayBetween(cmds)
}

def on(value) {
	log.debug "value $value"
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint: value, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint: value, commandClass:37, command:2).format()
	])
}

def off(value) {
	log.debug "value $value"
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint: value, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint: value, commandClass:37, command:2).format()
	])
}

def poll() {
	log.debug "Poll - Refreshing"
	refresh()
}

def configure() {
	log.debug "Executing 'configure'"
    delayBetween([
    	zwave.configurationV1.configurationSet(parameterNumber:101, size:4, configurationValue: [ 0, 0, 127, 127 ]).format(),	// Report meter on all channels (6 sockets + master)
        zwave.configurationV1.configurationSet(parameterNumber:111, size:4, scaledConfigurationValue: 120).format(),		// 120s interval for meter reports
        zwave.configurationV1.configurationSet(parameterNumber:4, configurationValue: [0]).format()				// Report reguarly
    ])
}

def on() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:37, command:2).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:50, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:50, command:1, parameter:[16]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:50, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:50, command:1, parameter:[16]).format(),
		zwave.meterV2.meterGet(scale:0).format(),
    	zwave.meterV2.meterGet(scale:2).format()
	])
}

def off() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:37, command:2).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:50, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:50, command:1, parameter:[16]).format(),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:50, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:3, commandClass:50, command:1, parameter:[16]).format(),
		zwave.meterV2.meterGet(scale:0).format(),
    	zwave.meterV2.meterGet(scale:2).format()
    ])
}

//reset
def reset() {
	def cmds = []
    cmds << zwave.meterV2.meterReset().format()
    cmds << zwave.meterV2.meterGet(scale:0).format()
    for ( i in 1..6 ) 
    	cmds << zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:i, commandClass:50, command:1, parameter:[0]).format()

	delayBetween(cmds)
}