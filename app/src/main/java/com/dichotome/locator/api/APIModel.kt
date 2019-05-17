package com.dichotome.locator.api

object APIModel {
    data class Meta(
        val code: Int,
        val requestId: String
    )

    data class VenueResult(val meta: Meta, val response: VenueResponse)
    data class VenueResponse(val groups: Array<Group>)
    data class Group(val items: Array<Item>)
    data class Item(val venue: Venue)
    data class Venue(
        val id: String,
        val name: String,
        val location: VenueLocation
    )

    data class VenueLocation(
        val address: String,
        val lat: Double,
        val lng: Double
    )

    data class PictureResult(val meta: Meta? = null, val response: PictureResponse? = null)
    data class PictureResponse(val photos: Photos)
    data class Photos(val count: Int, val items: Array<PhotoItem>)
    data class PhotoItem(
        val prefix: String,
        val suffix: String
    )
}