/**
 *  Laundry Monitor v1.4 - 2018-07-23
 *
 *		Changelog
 *			v1.4	- Kill Contact Book because SmartThings removed the feature
 *          v1.3    - Fixes for mid-cycle checks
 *			v1.2	- Switch to ST Contact Book for notifications
 *			v1.1 	- Added minimumOnTime, works better for faster reporting power meters
 *			v1.0	- Initial releases never versioned
 *
 *  Copyright 2018 Brandon Miller
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
 
import groovy.time.* 
 
definition(
    name: "Laundry Monitor",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "Monitor power usage for laundry machines and notify when cycle end has been detected.",
    category: "Convenience",
    iconUrl: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png",
    iconX2Url: "http://www.vivevita.com/wp-content/uploads/2009/10/recreation_sign_laundry.png")


preferences {
	section("Tell me when this washer/dryer has stopped..."){
		input "sensor1", "capability.powerMeter"
	}
    
    section("Notifications") {             
        input "sendSMS", "phone", title: "Alert with SMS message?",description: "Phone Number", required: false
        input "sendPush", "bool", required: false, title: "Alert with Push Notification?"
        input "message", "text", title: "Notification message", description: "Laundry is done!", required: true
	}

	section("System Variables"){
    	input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 50
        input "minimumOnTime", "decimal", title: "Minimum amount of above wattage time to signal cycle start (secs)", required: false, defaultValue: 60
        input "minimumOffTime", "decimal", title: "Minimum amount of below wattage time to trigger off (secs)", required: false, defaultValue: 60
	}
	
	section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
        input "switches", "capability.switch", title: "Turn on these switches?", required:false, multiple:true
	    input "speech", "capability.speechSynthesis", title:"Speak message via: ", multiple: true, required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "power", powerInputHandler)
    atomicState.startedAt = null
    atomicState.stoppedAt = null
    atomicState.midCycleTime = null
    atomicState.isRunning = false
    atomicState.realStart = false
}

def powerInputHandler(evt) {
	def latestPower = sensor1.currentValue("power")
    // Null atomicState, nothing has happened in a while
    if ((!atomicState.isRunning || atomicState.isRunning == null) && latestPower > minimumWattage && atomicState.startedAt == null) {
    	atomicState.isRunning = true
    	atomicState.startedAt = now()
        atomicState.stoppedAt = null
        atomicState.midCycleTime = null
        atomicState.realStart = false
        log.trace "Machine has woken up from slumber."
    }
    // Confirming a real cycle start and not a wrinkle cycle or random power spike
    else if (atomicState.isRunning && latestPower > minimumWattage && ((now() - atomicState.startedAt)/1000 > minimumOnTime) && !atomicState.realStart) {
        atomicState.realStart = true
        log.trace "Cycle started at ${atomicState.startedAt}"
    }
    // Cycle has started, power has dipped below minimum watt threshold
    else if (atomicState.isRunning && latestPower < minimumWattage && atomicState.realStart) {
    	if (atomicState.midCycleTime == null)
        {
            atomicState.midCycleTime = now()
        }
        else
        {
        	// Time between first check and now  
            if ((now() - atomicState.midCycleTime)/1000 > minimumOffTime)
            {
            	atomicState.isRunning = false                
                atomicState.stoppedAt = now()  
                log.trace "startedAt: ${atomicState.startedAt}, stoppedAt: ${atomicState.stoppedAt}"      
                atomicState.startedAt = null
                atomicState.realStart = false
                atomicState.midCycleTime = null
                
                log.info message
                
                
                if (sendSMS) {
                    sendSms(phone, message)
                }
                if (sendPush) {
                	sendPush(message)
                }
                if (switches) {
          			switches*.on()
      			}               
                if (speech) { 
                    speech.speak(message) 
                }          
            }
        }             	
    } 
    // Reset midCycleTime if we started back up
    else if (atomicState.isRunning && latestPower > minimumWattage && atomicState.realStart) {
        atomicState.midCycleTime = null
    }
    // False start, reset
    else if (atomicState.isRunning && latestPower < minimumWattage && !atomicState.realStart) { 	
        atomicState.isRunning = false
        atomicState.startedAt = null
    }
}

private hideOptionsSection() {
  (phone || switches) ? false : true
}