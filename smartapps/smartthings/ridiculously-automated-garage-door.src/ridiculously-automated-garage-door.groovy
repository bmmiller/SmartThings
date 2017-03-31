/**
 *  Ridiculously Automated Garage Door
 *
 *  Author: SmartThings
 *  Date: 2013-03-10
 *  Updated: 2015-03-19
 *
 * Monitors arrival and departure of car(s) and
 *
 *    1) opens door when car arrives,
 *    REMOVED 2) closes door after car has departed (for N minutes),
 *    REMOVED 3) opens door when car door motion is detected,
 *    REMOVED 4) closes door when door was opened due to arrival and interior door is closed.
 *
 *	  Heavily modified for simplification.  Mostly just wanted the false alarm functionality.
 */

definition(
    name: "Ridiculously Automated Garage Door",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Monitors arrival and departure of car(s) and 1) opens door when car arrives, 2) closes door after car has departed (for N minutes), 3) opens door when car door motion is detected, 4) closes door when door was opened due to arrival and interior door is closed.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {

	section("Garage door") {
		input "doorSensor", "capability.contactSensor", title: "Which sensor?"
		input "doorSwitch", "capability.switch", title: "Which switch?"		
	}
	section("Presence(s) using this garage door") {
		input "presences", "capability.presenceSensor", title: "Presence sensor", description: "Which presence sensor(s)?", multiple: true, required: false
	}   
    section ("Notifications") {
    	input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
        input "openThreshold", "number", title: "Warn when open longer than...",description: "Number of minutes", required: false
    }
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "number", title: "Number of minutes", required: false
	}
    section("Allow Garage Opening Timeframe...") {
		input name: "time0", title: "Start Time?", type: "time"
        input name: "time1", title: "End Time?", type: "time"
	}
}

def installed() {
	log.trace "installed()"
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	log.debug "present: ${presences.collect{it.displayName + ': ' + it.currentPresence}}"
	subscribe(doorSensor, "contact", garageDoorContact)
	subscribe(presences, "presence", carPresence)
}

def doorOpenCheck()
{
	final thresholdMinutes = openThreshold
	if (thresholdMinutes) {
		def currentState = doorSensor.contactState
		log.debug "doorOpenCheck"
		if (currentState?.value == "open") {
			log.debug "open for ${now() - currentState.date.time}, openDoorNotificationSent: ${state.openDoorNotificationSent}"
			if (!state.openDoorNotificationSent && now() - currentState.date.time > thresholdMinutes * 60 * 1000) {
				def msg = "${doorSwitch.displayName} was been open for ${thresholdMinutes} minutes"
				log.info msg
                
				if (location.contactBookEnabled && recipients) {
                    log.debug "Contact Book enabled!"
                    sendNotificationToContacts(message, recipients)
                } else {
                    log.debug "Contact Book not enabled"
                    if (phone) {
                        sendSms(phone, message)
                    }
                }		
                    
				state.openDoorNotificationSent = true
			}
		}
	}
}

def carPresence(evt)
{
	// time in which there must be no "not present" events in order to open the door
	final openDoorAwayInterval = falseAlarmThreshold ? falseAlarmThreshold * 60 : 600

	if ( evt.value == "present" && now() > timeToday(time0).time && now() < timeToday(time1).time ) {
		// A car comes home

		def car = getPresence(evt)
		def t0 = new Date(now() - (openDoorAwayInterval * 1000))
		def states = car.statesSince("presence", t0)
		def recentNotPresentState = states.find{it.value == "not present"}

		if (recentNotPresentState) {
			log.debug "Not opening ${doorSwitch.displayName} since car was not present at ${recentNotPresentState.date}, less than ${openDoorAwayInterval} sec ago"
		}
		else {
			if (doorSensor.currentContact == "closed") {
				openDoor()
                if (location.contactBookEnabled && recipients) {
                    log.debug "Contact Book enabled!"
                    sendNotificationToContacts("Opening garage door due to arrival of ${car.displayName}", recipients)
                } else {
                    log.debug "Contact Book not enabled"
                    if (phone) {
                        sendSms(phone, "Opening garage door due to arrival of ${car.displayName}")
                    }
                }
				state.appOpenedDoor = now()
			}
			else {
				log.debug "door already open"
			}
		}
	}
}

def garageDoorContact(evt)
{
	log.info "garageDoorContact, $evt.name: $evt.value"
	if (evt.value == "open") {
		schedule("0 * * * * ?", "doorOpenCheck")
	}
	else {
		unschedule("doorOpenCheck")
        state.openDoorNotificationSent = false      
	}
}

private openDoor()
{
	if (doorSensor.currentContact == "closed") {
		log.debug "opening door"
		doorSwitch.push()
	}
}

private closeDoor()
{
	if (doorSensor.currentContact == "open") {
		log.debug "closing door"
		doorSwitch.push()
	}
}

private getPresence(evt)
{
	presences.find{it.id == evt.deviceId}
}
