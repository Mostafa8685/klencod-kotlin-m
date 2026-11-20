package com.apk3rade.mangoide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileTreeAdapter(
    private val onSelect: (FileNode) -> Unit,
    private val onLongPress: (FileNode) -> Unit
) : RecyclerView.Adapter<FileTreeAdapter.NodeVH>() {

    private var items: List<FileNode> = emptyList()

    fun submitList(nodes: List<FileNode>) {
        items = nodes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file_node, parent, false)
        return NodeVH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: NodeVH, position: Int) {
        val node = items[position]
        val padStart = (8 + node.depth * 14) * holder.itemView.resources.displayMetrics.density
        holder.itemView.setPaddingRelative(padStart.toInt(), 0, 8, 0)
        holder.tvName.text = node.file.name
        holder.ivIcon.setImageResource(if (node.isDirectory) R.drawable.ic_folder else R.drawable.ic_file)
        holder.itemView.setOnClickListener { onSelect(node) }
        holder.itemView.setOnLongClickListener {
            onLongPress(node)
            true
        }
    }

    class NodeVH(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivNodeIcon)
        val tvName: TextView = view.findViewById(R.id.tvNodeName)
    }
}
