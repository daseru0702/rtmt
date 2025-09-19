package app.rtmt.rtmt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RtmtApplication

fun main(args: Array<String>) {
    runApplication<RtmtApplication>(*args)
}
