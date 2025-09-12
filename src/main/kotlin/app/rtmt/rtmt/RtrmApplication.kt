package app.rtmt.rtmt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RtrmApplication

fun main(args: Array<String>) {
    runApplication<RtrmApplication>(*args)
}
