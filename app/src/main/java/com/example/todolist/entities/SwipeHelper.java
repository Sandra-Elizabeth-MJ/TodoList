package com.example.todolist.entities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.adapters.TareaAdapter;
import com.example.todolist.services.FirestoreManager;

public class SwipeHelper extends ItemTouchHelper.SimpleCallback {
    private static final String TAG = "SwipeHelper";
    private final TareaAdapter adapter;
    private final Context context;
    private final FirestoreManager firestoreManager;
    private final Paint p = new Paint();
    private final float buttonWidth;
    private boolean swipeBack = false;
    private ButtonsState buttonShowedState = ButtonsState.GONE;
    private static final float buttonWidthDP = 80f;
    private RectF buttonInstance = null;
    private RecyclerView.ViewHolder currentItemViewHolder = null;

    enum ButtonsState {
        GONE,
        LEFT_VISIBLE
    }

    public SwipeHelper(TareaAdapter adapter, Context context, FirestoreManager firestoreManager) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.context = context;
        this.firestoreManager = firestoreManager;
        this.buttonWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                buttonWidthDP,
                context.getResources().getDisplayMetrics());
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (buttonShowedState != ButtonsState.GONE) {
                if (buttonShowedState == ButtonsState.LEFT_VISIBLE) dX = Math.max(dX, -buttonWidth * 3);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            else {
                setTouchListener(recyclerView, viewHolder);
            }

            if (buttonShowedState == ButtonsState.GONE) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        currentItemViewHolder = viewHolder;
        drawButtons(c, currentItemViewHolder);
    }

    private void drawButtons(Canvas c, RecyclerView.ViewHolder viewHolder) {
        View itemView = viewHolder.itemView;
        float height = itemView.getHeight();

        // Estrella
        p.setColor(ContextCompat.getColor(context, R.color.grayBackground));
        RectF starButton = new RectF(itemView.getRight() - 3 * buttonWidth, itemView.getTop(),
                itemView.getRight() - 2 * buttonWidth, itemView.getBottom());
        c.drawRect(starButton, p);

        // Fecha
        p.setColor(ContextCompat.getColor(context, R.color.blueBackground));
        RectF dateButton = new RectF(itemView.getRight() - 2 * buttonWidth, itemView.getTop(),
                itemView.getRight() - buttonWidth, itemView.getBottom());
        c.drawRect(dateButton, p);

        // Eliminar
        p.setColor(ContextCompat.getColor(context, R.color.redBackground));
        RectF deleteButton = new RectF(itemView.getRight() - buttonWidth, itemView.getTop(),
                itemView.getRight(), itemView.getBottom());
        c.drawRect(deleteButton, p);

        float iconMargin = (height - 24) / 2;

        // Iconos...
        drawIcons(c, itemView, iconMargin);
    }

    private void drawIcons(Canvas c, View itemView, float iconMargin) {
        // Icono estrella
        Drawable starIcon = ContextCompat.getDrawable(context, R.drawable.ic_star);
        if (starIcon != null) {
            starIcon.setBounds(
                    (int)(itemView.getRight() - 2.5f * buttonWidth - 12),
                    (int)(itemView.getTop() + iconMargin),
                    (int)(itemView.getRight() - 2.5f * buttonWidth + 12),
                    (int)(itemView.getBottom() - iconMargin)
            );
            starIcon.draw(c);
        }

        // Icono fecha
        Drawable dateIcon = ContextCompat.getDrawable(context, R.drawable.ic_calendar_white);
        if (dateIcon != null) {
            dateIcon.setBounds(
                    (int)(itemView.getRight() - 1.5f * buttonWidth - 12),
                    (int)(itemView.getTop() + iconMargin),
                    (int)(itemView.getRight() - 1.5f * buttonWidth + 12),
                    (int)(itemView.getBottom() - iconMargin)
            );
            dateIcon.draw(c);
        }

        // Icono eliminar
        Drawable deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        if (deleteIcon != null) {
            deleteIcon.setBounds(
                    (int)(itemView.getRight() - 0.5f * buttonWidth - 12),
                    (int)(itemView.getTop() + iconMargin),
                    (int)(itemView.getRight() - 0.5f * buttonWidth + 12),
                    (int)(itemView.getBottom() - iconMargin)
            );
            deleteIcon.draw(c);
        }
    }

    private void setTouchListener(RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder) {
        recyclerView.setOnTouchListener((v, event) -> {
            swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;

            if (swipeBack) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handleTouchUp(event, viewHolder);
                }
                // Reset swipeBack after handling the event
                swipeBack = false;
            }
            return false;
        });
    }

    private void handleTouchUp(MotionEvent event, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder == null || viewHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
            Log.e(TAG, "Invalid ViewHolder or position in handleTouchUp");
            resetSwipeState();
            return;
        }

        View itemView = viewHolder.itemView;
        float x = event.getRawX();
        float itemRight = itemView.getRight();

        int[] location = new int[2];
        itemView.getLocationOnScreen(location);
        float itemX = location[0];
        float relativeX = x - itemX;

        if (buttonShowedState == ButtonsState.LEFT_VISIBLE) {
            Tarea tarea = adapter.getTareaAt(viewHolder.getAdapterPosition());

            if (tarea == null) {
                Log.e(TAG, "Null tarea object at position: " + viewHolder.getAdapterPosition());
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                resetSwipeState();
                return;
            }

            try {
                if (relativeX >= itemRight - buttonWidth) {
                    deleteTarea(tarea, viewHolder.getAdapterPosition());
                } else if (relativeX >= itemRight - 2 * buttonWidth) {
                    handleDateButton(viewHolder.getAdapterPosition());
                } else if (relativeX >= itemRight - 3 * buttonWidth) {
                    handleStarButton(viewHolder.getAdapterPosition());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling touch up event", e);
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                resetSwipeState();
            }
        }
    }

    private void resetSwipeState() {
        buttonShowedState = ButtonsState.GONE;
        swipeBack = false;
        if (currentItemViewHolder != null) {
            adapter.notifyItemChanged(currentItemViewHolder.getAdapterPosition());
        }
    }

    private void handleDateButton(int position) {
        try {
            Toast.makeText(context, "Botón Fecha presionado", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
            resetSwipeState();
        } catch (Exception e) {
            Log.e(TAG, "Error handling date button", e);
            resetSwipeState();
        }
    }

    private void handleStarButton(int position) {
        try {
            Toast.makeText(context, "Botón Estrella presionado", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
            resetSwipeState();
        } catch (Exception e) {
            Log.e(TAG, "Error handling star button", e);
            resetSwipeState();
        }
    }

    private void deleteTarea(Tarea tarea, int position) {
        if (tarea == null) {
            Log.e(TAG, "Attempted to delete null tarea");
            adapter.notifyItemChanged(position);
            resetSwipeState();
            return;
        }

        String tareaId = tarea.getId();
        if (tareaId == null || tareaId.isEmpty()) {
            Log.e(TAG, "Invalid tarea ID");
            Toast.makeText(context, "Error: ID de tarea inválido", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(position);
            resetSwipeState();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Eliminar Tarea")
                    .setMessage("¿Estás seguro de que deseas eliminar esta tarea?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        firestoreManager.deleteTarea(tareaId, new FirestoreManager.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                try {
                                    adapter.removeTarea(position);
                                    Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                                    resetSwipeState();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error removing tarea from adapter", e);
                                    adapter.notifyItemChanged(position);
                                    resetSwipeState();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error deleting tarea from Firestore", e);
                                adapter.notifyItemChanged(position);
                                Toast.makeText(context, "Error al eliminar la tarea", Toast.LENGTH_SHORT).show();
                                resetSwipeState();
                            }
                        });
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        adapter.notifyItemChanged(position);
                        resetSwipeState();
                    })
                    .setOnCancelListener(dialog -> {
                        adapter.notifyItemChanged(position);
                        resetSwipeState();
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog", e);
            adapter.notifyItemChanged(position);
            resetSwipeState();
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (viewHolder.getAdapterPosition() == RecyclerView.NO_POSITION) {
            Log.e(TAG, "Invalid position in onSwiped");
            resetSwipeState();
            return;
        }

        if (direction == ItemTouchHelper.LEFT) {
            buttonShowedState = ButtonsState.LEFT_VISIBLE;
        }
    }
}