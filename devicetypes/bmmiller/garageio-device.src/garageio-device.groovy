/**
 *  Garageio Device v1.6 - 2022-01-29
 *
 * 		Changelog
 *			v1.6	- Updates for New App
 *			v1.5	- Remove Garage Door Control which was deprecated
 *			v1.4.1	- Another small fix for polling and ActionTiles
 *			v1.4	- Fixes for ActionTiles compatibility
 *			v1.3.3	- Compatibility Update
 *			v1.3.2  - Update for new SmartThings blue dominant theme
 *			v1.3.1	- Added Garage Door Control capability so it will be usable for standard Garage Door SmartApps
 *			v1.3 	- Added watchdogTask() to restart polling if it stops, inspiration from Pollster
 *			v1.2    - Added multiAttributeTile() to make things look prettier. No functionality change.
 *			v1.1.1 	- Tiny fix for service manager failing to complete
 *			v1.1    - GarageioServiceMgr() and Device Handler impplemented to handle ChildDevice creation, deletion, 
 *				      polling, and ST suggested implementation.
 *			v1.0    - GarageioInit() implementation to handle polling in a Smart App, left this way for quite a while
 *			v0.1    - Initial working integration
 *
 *  Copyright 2016 Brandon Miller
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
	definition (name: "Garageio Device", namespace: "bmmiller", author: "Brandon Miller") {
		capability "Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Sensor"
        capability "Polling"
        capability "Relay Switch"
        capability "Momentary"
        attribute "status", "string"
    }
    
    tiles(scale:2) {
    	multiAttributeTile(name:"summary", type: "generic", width: 6, height: 4){
			tileAttribute ("device.door", key: "PRIMARY_CONTROL") {
                attributeState("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00a0dc", nextState:"opening")
                attributeState("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
                attributeState("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#e86d13")
                attributeState("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#00a0dc")
            }
        }
        
        standardTile("toggle", "device.door", width: 4, height: 4) {
			state("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00A0DC", nextState:"opening")
			state("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#e86d13")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#00A0DC")	
		}
        
        standardTile("open", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"close", icon:"st.doors.garage.garage-closing"
		}       
        standardTile("refresh", "device", width: 2, height: 2, inactiveLabel: false, decoration: "flat" ) {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
		
        main "summary"
		details(["summary", "open", "close", "refresh"])
	}
}

def refresh() {
	log.trace "Refresh called"
    poll()
}

def poll() {
	log.debug "Executing poll()"   
    def results = parent.pollChildren()
    parsePollData(results)
}

def parsePollData(results) {
	log.debug "Results: " + results    
    //log.debug "Door ID to Update: " + device.deviceNetworkId
    if (results != null) {
        results.each {
            if (it.id == device.deviceNetworkId)
            {
                updateStatus(it.state)
            }    
        }   
    }
    else {
    	log.debug "Something went wrong, results of poll() were null."
	}
}

def updateStatus(status) {
	if (status == "CLOSED")
    {    	
        sendEventClosed()
    }
    else if (status == "OPEN")
    {
    	sendEventOpen()
    }
    log.debug "Status Before Poll for Door Id ${device.deviceNetworkId}: ${state.status}, Status After Poll: ${status}"
    // Now update
    state.status = status
}

def open() {
	if (state.status == "CLOSED") {
    	log.debug "open(): Opening door"
        push()
        sendEventOpen()
    } else {
        log.debug "We're already open, doing nothing"
    }
}

def close() {
	if (state.status == "OPEN") {
    	log.debug "close(): Closing door"
		push()
        sendEventClosed()
    } else {
    	log.debug "We're already closed, doing nothing"
    }
}

def push() { 
    def changeState = (state.status == "OPEN") ? "CLOSED" : "OPEN"
    log.debug "Current State: ${state.status}, Change to State: ${changeState}, Door ID: ${device.deviceNetworkId}"
    log.debug "Call parent.push(dni, changeState)"
    
    def result = parent.push(device.deviceNetworkId, changeState)
    log.debug result
    if (result == 200) {
    	log.info "Push was successful.  Polling in 60 seconds..."
    	runIn(60, parent.pollChildren()) 
    } else if (result == 401) {
    	log.error "Authentication error, need to get/refresh token."
    }
}

def sendEventOpen() {
	log.info "sendEventOpen()"
	sendEvent(name: 'status', value: 'open')
    sendEvent(name: 'state', value: 'open')
    sendEvent(name: 'door', value: 'open')
    sendEvent(name: 'contact', value: 'open')
}

def sendEventClosed() {
	log.info "sendEventClosed()"
    sendEvent(name: 'status', value: 'closed')
    sendEvent(name: 'state', value: 'closed')
    sendEvent(name: 'door', value: 'closed')
    sendEvent(name: 'contact', value: 'closed')
}