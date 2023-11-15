package tornadofxbuilders

import javafx.util.Duration

val Number.millis: Duration get() = Duration.millis(this.toDouble())
val Number.seconds: Duration get() = Duration.seconds(this.toDouble())
val Number.minutes: Duration get() = Duration.minutes(this.toDouble())
val Number.hours: Duration get() = Duration.hours(this.toDouble())

operator fun Duration.plus(duration: Duration): Duration = this.add(duration)
operator fun Duration.minus(duration: Duration): Duration = this.subtract(duration)
