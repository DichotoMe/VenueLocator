package com.dichotome.locator.utils


import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.dichotome.locator.api.APIModel
import com.dichotome.locator.ui.MapInfoFragment
import com.dichotome.locator.ui.MapInfoFragment.Companion.ADDRESS
import com.dichotome.locator.ui.MapInfoFragment.Companion.PIC_ID
import com.dichotome.locator.ui.MapInfoFragment.Companion.TITLE

class LocationsPagerAdapter(private val items: Array<APIModel.Item>, fm: FragmentManager) :
    FragmentStatePagerAdapter(fm) {

    val pageReferenceMap = mutableMapOf<Int, Fragment>()

    override fun getItem(position: Int) = MapInfoFragment().apply {
        items[position].venue.apply {
            arguments = bundleOf(
                TITLE to name,
                ADDRESS to location.address,
                PIC_ID to id
            )
        }
    }.also {
        pageReferenceMap.put(position, it)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        pageReferenceMap.remove(position)
    }

    override fun getCount() = items.size
}