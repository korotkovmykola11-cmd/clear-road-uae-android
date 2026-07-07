package com.clearroad.app.ui.driveweather

/**
 * Drive Signature resolves from the same Hero state as mood copy — never independently in production.
 */
fun DriveWeatherHero.resolvedDriveSignature(): DriveSignature =
    when (trafficCharacter) {
        TripTrafficCharacter.STOP_START -> DriveSignature.STOP_START
        TripTrafficCharacter.DETOUR -> DriveSignature.BUSY
        TripTrafficCharacter.FLOWING -> DriveSignature.SMOOTH
        TripTrafficCharacter.CONGESTED -> DriveSignature.CONGESTED
        null -> mood.toDefaultDriveSignature()
    }
