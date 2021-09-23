package com.droidknights.app2021.home.ui.adapter

import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.droidknights.app2021.home.R
import com.droidknights.app2021.home.databinding.ItemInfoHeaderBinding
import com.droidknights.app2021.home.ui.SponsorItemDecoration
import com.droidknights.app2021.home.util.DataBindingViewHolder
import com.droidknights.app2021.home.util.recyclerview.ItemDiffCallback
import com.droidknights.app2021.home.util.recyclerview.ListBindingAdapter
import com.droidknights.app2021.shared.model.Sponsor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCROLL_DX = 5

internal class InfoAdapter(
    private val coroutineScope: CoroutineScope,
    sponsors: List<Sponsor>,
    private val itemHandler: ItemHandler
) : ListBindingAdapter<InfoItem>(ItemDiffCallback(
    onItemsTheSame = { old, new -> old.sponsors == new.sponsors },
    onContentsTheSame = { old, new -> old == new }
)) {

    private val sponsorItemHandler = object : SponsorAdapter.ItemHandler {
        override fun clickSponsor(sponsor: Sponsor) {
            itemHandler.clickSponsor(sponsor)
        }
    }

    init {
        submitList(listOf(InfoItem(sponsors)))
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.item_info_header
    }

    override fun viewBindViewHolder(holder: DataBindingViewHolder<InfoItem>, position: Int) {
        super.viewBindViewHolder(holder, position)

        with(holder.binding as ItemInfoHeaderBinding) {
            sponsorList.adapter = SponsorAdapter(getItem(0).sponsors, sponsorItemHandler)
            sponsorList.addItemDecoration(SponsorItemDecoration())
        }

        var scrollJob: Job? = coroutineScope.launch {
            holder.binding.sponsorList.launchAutoScroll()
        }

        holder.binding.sponsorList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        scrollJob = coroutineScope.launch {
                            holder.binding.sponsorList.launchAutoScroll()
                        }
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        scrollJob?.cancel()
                    }
                }
            }
        })

        holder.binding.sponsorList.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    scrollJob?.cancel()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    scrollJob = coroutineScope.launch {
                        holder.binding.sponsorList.launchAutoScroll()
                    }
                    true
                }
                else -> true
            }
        }
    }

    interface ItemHandler {
        fun clickSponsor(sponsor: Sponsor)
    }

    private tailrec suspend fun RecyclerView.launchAutoScroll() {
        smoothScrollBy(SCROLL_DX, 0)
        val firstVisibleItem =
            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            val currentList = (adapter as SponsorAdapter).currentList
            val secondPart = currentList.subList(0, firstVisibleItem)
            val firstPart = currentList.subList(firstVisibleItem, currentList.size)
            (adapter as SponsorAdapter).submitList(firstPart + secondPart)
        }
        delay(25L)
        launchAutoScroll()
    }
}

data class InfoItem(val sponsors: List<Sponsor>)