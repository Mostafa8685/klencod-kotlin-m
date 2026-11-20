package com.apk3rade.mangoide

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class FileTreeAdapter(private val context: Context) : BaseAdapter() {

    private var items: List<FileNode> = emptyList()

    fun submitList(nodes: List<FileNode>) {
        items = nodes
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): FileNode = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_file_node, parent, false)
        val node = items[position]
        val padStart = ((8 + node.depth * 14) * context.resources.displayMetrics.density).toInt()
        view.setPaddingRelative(padStart, 0, 8, 0)
        view.findViewById<ImageView>(R.id.ivNodeIcon).setImageResource(
            if (node.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
        )
        view.findViewById<TextView>(R.id.tvNodeName).text = node.file.name
        return view
    }
}
