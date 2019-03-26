let music_player_launched = false
input.onButtonPressed(Button.A, function () {
    if (!(music_player_launched)) {
        music_player_launched = true
        devices.tellRemoteControlTo(MesRemoteControlEvent.play)
        basic.showLeds(`
            . . # . .
            . . . # .
            # # # # #
            . . . # .
            . . # . .
            `)
    }
})
input.onButtonPressed(Button.B, function () {
    if (music_player_launched) {
        devices.tellRemoteControlTo(MesRemoteControlEvent.nextTrack)
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
    basic.showLeds(`
        . . # . .
        . # . . .
        # # # # #
        . # . . .
        . . # . .
        `)
    music_player_launched = false
    devices.tellRemoteControlTo(MesRemoteControlEvent.stop)
})
music_player_launched = false
basic.showLeds(`
    . . # . .
    . # . . .
    # # # # #
    . # . . .
    . . # . .
    `)
