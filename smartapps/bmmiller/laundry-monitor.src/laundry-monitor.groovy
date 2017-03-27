/**
 *  Laundry Monitor v1.1 - 2017-03-27
 *
 *		Changelog
 *			v1.1 	- Added minimumOnTime, works better for faster reporting power meters
 *			v1.0	- Initial releases never versioned
 *
 *  Copyright 2017 Brandon Miller
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
		input "sendPushMessage", "bool", title: "Push Notifications?"
		input "phone", "phone", title: "Send a text message?", required: false
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"      
	}

	section("System Variables"){
    	input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 50
        input "minimumOnTime", "decimal", title: "Minimum amount of above wattage time to signal cycle start (secs)", required: false, defaultValue: 60
        input "minimumOffTime", "decimal", title: "Minimum amount of below wattage time to trigger off (secs)", required: false, defaultValue: 60
        input "message", "text", title: "Notification message", description: "Laundry is done!", required: true
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
}

def powerInputHandler(evt) {
	def latestPower = sensor1.currentValue("power")
    log.trace "Power: ${latestPower}W"
    
    // Null state, nothing has happened in a while
    if (!state.isRunning && latestPower > minimumWattage && state.startedAt == null) {
    	state.isRunning = true
    	state.startedAt = now()
        state.stoppedAt = null
        state.midCycleCheck = null
        state.realStart = false
    }
    // Confirming a real cycle start and not a wrinkle cycle or random power spike
    else if (state.isRunning && latestPower > minimumWattage && ((now() - state.startedAt)/1000 > minimumOnTime)) {
        state.realStart = true
        log.trace "Cycle started."
    }
    // Cycle has started, power has dipped below minimum watt threshold
    else if (state.isRunning && latestPower < minimumWattage && state.realStart) {
    	if (state.midCycleCheck == null)
        {
        	state.midCycleCheck = true
            state.midCycleTime = now()
        }
        else if (state.midCycleCheck == true)
        {
        	// Time between first check and now  
            if ((now() - state.midCycleTime)/1000 > minimumOffTime)
            {
            	state.isRunning = false                
                state.stoppedAt = now()  
                log.debug "startedAt: ${state.startedAt}, stoppedAt: ${state.stoppedAt}"      
                state.startedAt = null
                log.info message
                
                if (phone) {
                    if ( phone.indexOf(";") > 1){
                        def phones = phone.split(";")
                        for ( def i = 0; i < phones.size(); i++) {
                            sendSms(phones[i], message)
                        }
                    } else {
                        sendSms(phone, message)
                    }
                }
                
                if (sendPushMessage) {
                    sendPush message
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
    // False start, reset
    else if (state.isRunning && latestPower < minimumWattage && !state.realStart) { 	
        state.isRunning = false
        state.startedAt = null
    }
}

private hideOptionsSection() {
  (phone || switches) ? false : true
}