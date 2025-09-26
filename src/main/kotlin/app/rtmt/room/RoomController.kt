// src/main/kotlin/app/rtmt/room/RoomController.kt
package app.rtmt.room

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rooms")
class RoomController(private val rooms: RoomService) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = rooms.findRoom(id)
}
