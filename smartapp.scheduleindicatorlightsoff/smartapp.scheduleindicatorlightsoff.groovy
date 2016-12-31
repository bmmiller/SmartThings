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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Switches to turn off indicator lights on..."){
        input "indicators0", "capability.indicator", multiple: true, required: false, title:"Indicator On when On"
        input "indicators1", "capability.indicator", multiple: true, required: false, title:"Indicator On when Off"
    }
    
    section("Between these times...") {
        input "time0", "time", title: "From what time?"
        input "time1", "time", title: "Until what time?"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    schedule(time0, "turnOff")
    schedule(time1, "turnOn")
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    schedule(time0, "turnOff")
    schedule(time1, "turnOn")
}

def turnOff() {
    log.debug "Turning indicator lights off"
    
    if (indicators0)
        indicators0.indicatorNever()
    if (indicators1)
        indicators1.indicatorNever()
}

def turnOn() {
    log.debug "Return indicator lights to original state"
    
    if (indicators0)
        indicators0.indicatorWhenOn()
    if (indicators1)
        indicators1.indicatorWhenOff()
}


