package com.example.firstproject;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;


public class MyAdapter extends FirestoreRecyclerAdapter<ListItem, MyAdapter.ViewHolder> {

    private FirestoreRecyclerOptions<ListItem> list;
    private Context context;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public MyAdapter(@NonNull FirestoreRecyclerOptions<ListItem> options) {
        super(options);
        list = options;
    }


    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull ListItem model) {
        ListItem li = list.getSnapshots().get(position);

        holder.name.setText(li.getName());
        holder.price.setText("cena: " + li.getPrice() + " zł");
        holder.count.setText("ilość: " + li.getCount());

        if(li.isBought()){
            holder.bought.setChecked(true);
        }else{
            holder.bought.setChecked(false);
        }
    }


    public Context getContext() {
        return context;
    }



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView name;
        public TextView price;
        public TextView count;
        public CheckBox bought;

        public ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.tvName);
            price = view.findViewById(R.id.tvPrice);
            count = view.findViewById(R.id.tvCount);
            bought = view.findViewById(R.id.cbBought);

            view.setOnClickListener(this);


        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(context, EditItemActivity.class);
            intent.putExtra("itemPosition", getAdapterPosition());
            context.startActivity(intent);
        }


    }
}
