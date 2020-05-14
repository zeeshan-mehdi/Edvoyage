package com.advoyage.livestream.testekin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.advoyage.livestream.testekin.R;
import com.advoyage.livestream.testekin.model.Message;
import com.advoyage.livestream.testekin.model.Schedule;

import java.util.List;

public class StreamScheduleListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<Schedule> mScheduleList;

    public StreamScheduleListAdapter(Context context, List<Schedule> messageList) {
        mContext = context;
        mScheduleList = messageList;
    }

    public void notifyDataSetChanged(List<Schedule> messages) {
        this.mScheduleList = messages;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mScheduleList.size();
    }


    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_stream_item, parent, false);
        return new ScheduleHolder(view);

    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Schedule message = (Schedule) mScheduleList.get(position);
        ((ScheduleHolder) holder).bind(message);
    }


    private static class ScheduleHolder extends RecyclerView.ViewHolder {
        TextView nameText, timeText;

        ScheduleHolder(View itemView) {
            super(itemView);

            nameText = (TextView) itemView.findViewById(R.id.streamName);
            timeText = (TextView) itemView.findViewById(R.id.streamTime);
        }

        void bind(Schedule message) {
            nameText.setText(message.name);
            timeText.setText(message.dateTime);

            // Format the stored timestamp into a readable String using method.
            // timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
        }
    }
}


