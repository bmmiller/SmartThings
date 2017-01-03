/**
 *  Schedule Indicator Light Off
 *
 *  Copyright 2015 Brandon Miller
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
definition(
    name: "Schedule Indicator Light Off",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "Schedule Indicator Light Off",
    category: "Convenience",
    iconUrl: "https://cdn1.iconfinder.com/data/icons/all_google_icons_symbols_by_carlosjj-du/128/alarm_light-b.png",
    iconX2Url: "https://cdn1.iconfinder.com/data/icons/all_google_icons_symbols_by_carlosjj-du/128/alarm_light-b.png",
    iconX3Url: "https://cdn1.iconfinder.com/data/icons/all_google_icons_symbols_by_carlosjj-du/128/alarm_light-b.png")


preferences {
    section("Switches to turn off indicator lights on..."){
        input "indicators0", "capability.indicator", multiple: true, required: false, title:"Indicator On when On"
        input "indicators1", "capability.indicator", multiple: true, required: false, title:"Indicator On when Off"
        input "time0", "time", required: false, title: "From what time?"
        input "time1", "time", required: false, title: "Until what time?"    
    }
    
    section("Devices Alarming..."){
        input "contactSensors", "capability.contactSensor", required: false, title: "Which sensor?"
        input "openThreshold", "number", title: "Alarm when open longer than...", description: "Number of minutes", required: false
        input "alarmIndicators","capability.indicator", multiple: true, required: false, title:"Indicator Light will be Used as Alarm"
    }   
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    schedule(time0, "turnOff")
    schedule(time1, "turnOn")
    subscribe(contactSensors, "contact", deviceContact)
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    schedule(time0, "turnOff")
    schedule(time1, "turnOn")
    subscribe(contactSensors, "contact", deviceContact)
}

def turnOff() {
    log.info "Turning indicator lights off"
    
    if (indicators0)
        indicators0.indicatorNever()
    if (indicators1)
       indicators1.indicatorNever()
}

def turnOn() {
    log.info "Return indicator lights to original state"
    
    if (indicators0)
        indicators0.indicatorWhenOn()
    if (indicators1)
        indicators1.indicatorWhenOff()
}

def deviceContact(evt)
{
    log.info "deviceContact, $evt.name: $evt.value"
    if (evt.value == "open") {
		schedule("* * * * * ?", "contactOpenCheck")
	}
	else {
		unschedule("contactOpenCheck")
        alarmIndicators.indicatorWhenOff()
	}
}

def contactOpenCheck()
{
    final thresholdMinutes = openThreshold
    if (thresholdMinutes) {
        def currentState = contactSensors.contactState
        log.info "contactOpenCheck"
        if (currentState?.value == "open") {
            log.debug "open for ${now() - currentState.date.time}"
            if (now() - currentState.date.time > thresholdMinutes * 60 *1000) {
                for(int i = 0; i < 5; i++) {
                	alarmIndicators.indicatorWhenOn()
    				pause(2000)
    				alarmIndicators.indicatorWhenOff()
                    pause(2000)
				}               
			}
		}
	}
}