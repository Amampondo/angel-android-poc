# Angel Android POC

Angel is an experimental Android safety app that tests whether a deliberate wrist-shake gesture can trigger an emergency alert faster than accessing a phone manually.

This repository is a proof of concept, not a production emergency service. It currently creates a local test alert only; it does not contact a security company or police.

## What the prototype proves

- Accelerometer monitoring can continue while the app is not visible by using a foreground service.
- A deliberate double-direction shake can be separated from ordinary movement with tunable thresholds.
- A detected gesture can trigger vibration, a local alert, and an observable event counter.

## Run it

1. Open the project in Android Studio.
2. Let Gradle sync and install the `app` configuration on a physical Android phone.
3. Grant notification permission when prompted.
4. Strap the phone securely to your wrist or forearm.
5. Tap **Arm detection** and perform two sharp, opposite-direction wrist movements.

The persistent notification shows that detection is active. Use **Test alert** to verify the alert path without moving the device.

## Current limitations

- Thresholds are experimental and must be calibrated with real movement data.
- The app does not yet transmit location or contact a response provider.
- Android may require the user to exempt the app from aggressive manufacturer-specific battery optimisation.
- A phone-on-wrist test does not validate smartwatch battery life or Samsung sensor integration.

## Safety

Do not rely on this prototype during an emergency. Always use established local emergency and private-security channels.

