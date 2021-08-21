package ca.cgagnier.wlednativeandroid

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import ca.cgagnier.wlednativeandroid.fragment.DeviceViewFragment

abstract class AbstractDeviceListAdapter<T : RecyclerView.ViewHolder?>(protected val deviceList: ArrayList<DeviceItem>) : RecyclerView.Adapter<T>() {

    protected lateinit var context: Context

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    fun switchContent(id: Int, fragment: Fragment) {
        if (context is MainActivity) {
            val mainActivity = context as MainActivity
            mainActivity.switchContent(id, fragment, DeviceViewFragment.TAG_NAME)
        }
    }

    override fun getItemCount() = deviceList.count()

    fun getAllItems(): ArrayList<DeviceItem> {
        return deviceList
    }

    protected fun getItemPosition(item: DeviceItem): Int? {
        for (i in 0 until deviceList.size) {
            if (item == deviceList[i]) {
                return i
            }
        }
        return null
    }

    fun itemChanged(item: DeviceItem) {
        val position = getItemPosition(item)
        if (position != null) {
            deviceList[position] = item
            notifyItemChanged(position)
        }
    }

    fun addItem(item: DeviceItem) {
        deviceList.add(item)
        notifyItemInserted(deviceList.size - 1)
    }

    fun removeItem(item: DeviceItem) {
        val position = getItemPosition(item)
        if (position != null) {
            deviceList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}