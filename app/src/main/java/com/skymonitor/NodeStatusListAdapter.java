package com.skymonitor;

import java.util.ArrayList;

import com.skymonitor.R.color;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

public class NodeStatusListAdapter extends BaseAdapter {
	
	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private ArrayList<NodeInfo> mNodeInfoList = new ArrayList<NodeInfo>();
	
    public NodeStatusListAdapter(Context context, ArrayList<NodeInfo> nodeInfoList)
    {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mNodeInfoList.clear();
        mNodeInfoList.addAll(nodeInfoList);
    }

	@Override
	public int getCount() {
		return mNodeInfoList.size();
	}

	@Override
	public Object getItem(int position) {
		return mNodeInfoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
    static class ViewHolder
    {
        CheckBox StatusIcon = null;
        TextView nodePublicKey = null;
        FrameLayout item = null;
        TextView keynumber = null;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Log.i("Nodestatusadapter ", ""+position);
		ViewHolder viewHolder = null;
        //if(convertView == null){
           viewHolder = new ViewHolder();

           convertView = mInflater.inflate(R.layout.nodelist, parent, false);
           //viewHolder.item = (FrameLayout) convertView.findViewById(R.id.list_child_item);
           viewHolder.nodePublicKey = (TextView) convertView.findViewById(R.id.nodepublickey);
           viewHolder.StatusIcon = (CheckBox) convertView.findViewById(R.id.statusbox);
           viewHolder.keynumber = (TextView) convertView.findViewById(R.id.keyNumber);
        //}
        
        if (mNodeInfoList != null && mNodeInfoList.size() > 0)
        {
            NodeInfo mItemInfo = mNodeInfoList.get(position);
            viewHolder.nodePublicKey.setText(mItemInfo.getNodePublicKey());
            viewHolder.keynumber.setText(""+(position + 1));
            //viewHolder.StatusIcon.setChecked(mItemInfo.getNodeStatus());
            if(mItemInfo.getNodeStatus() == 1)
               viewHolder.nodePublicKey.setTextColor(Color.BLACK);
            else if(mItemInfo.getNodeStatus() == 0)
            	viewHolder.nodePublicKey.setTextColor(Color.RED);
            else
            	viewHolder.nodePublicKey.setTextColor(Color.GRAY);
        }
		return convertView;
	}
	
	public void updateNodeInfoList(ArrayList<NodeInfo> nodeInfoList){
		Log.i("updateNodeInfoList ", ""+nodeInfoList.size());
		mNodeInfoList.clear();
		mNodeInfoList.addAll(nodeInfoList);
		notifyDataSetChanged();
	}
        
}
