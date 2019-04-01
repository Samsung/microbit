let camera_launched = false
input.onButtonPressed(Button.A, function () {
    if (!(camera_launched)) {
        camera_launched = true
        basic.showLeds(`
            . . # . .
            . . . # .
            # # # # #
            . . . # .
            . . # . .
            `)
        devices.tellCameraTo(MesCameraEvent.LaunchPhotoMode)
    }
})
input.onButtonPressed(Button.B, function () {
    if (camera_launched) {
        devices.tellCameraTo(MesCameraEvent.TakePhoto)
        basic.showLeds(`
            . . . . .
            . # . # .
            # # # # #
            . # . # .
            . . . . .
            `)
    }
})
input.onButtonPressed(Button.AB, function () {
    camera_launched = false
    devices.tellCameraTo(MesCameraEvent.StopPhotoMode)
    basic.showLeds(`
        . . # . .
        . # . . .
        # # # # #
        . # . . .
        . . # . .
        `)
})
camera_launched = false
basic.showLeds(`
    . . # . .
    . # . . .
    # # # # #
    . # . . .
    . . # . .
    `)
