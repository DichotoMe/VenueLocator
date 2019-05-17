package com.dichotome.locator.api

import com.google.android.gms.maps.model.LatLng

fun APIModel.VenueResult.extractItems() = response.groups[0].items

fun Array<APIModel.Item>.toLatLngArray() = map {
    it.toLanLng()
}

fun APIModel.Item.toLanLng() = venue.location.run { LatLng(lat, lng) }