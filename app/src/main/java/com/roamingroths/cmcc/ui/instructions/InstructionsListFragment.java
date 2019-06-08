package com.roamingroths.cmcc.ui.instructions;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.util.Preconditions;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.domain.Instruction;
import com.roamingroths.cmcc.utils.SimpleArrayAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.functions.BiConsumer;
import timber.log.Timber;

/**
 * Created by parkeroth on 11/13/17.
 */

public class InstructionsListFragment extends Fragment {

  private static boolean DEBUG = true;
  private static String TAG = InstructionsListFragment.class.getSimpleName();

  InstructionsCrudViewModel mViewModel;

  public InstructionsListFragment() {
    if (DEBUG) Log.v(TAG, "Construct");
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(this).get(InstructionsCrudViewModel.class);
    mViewModel.initialize(getArguments());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_instruction_edit, container, false);

    ListView instructionsView = view.findViewById(R.id.lv_instructions);
    SimpleArrayAdapter<InstructionContainer, ViewHolder> adapter = new SimpleArrayAdapter<>(
        getActivity(), R.layout.list_item_instruction,
        v -> new ViewHolder(v, (instruction, isChecked) -> mViewModel.updateInstruction(instruction, isChecked)),
        t -> {});
    List<InstructionContainer> values = new ArrayList<>();
    values.addAll(EXTRA_INSTRUCTIONS);
    for (Instruction instruction : Instruction.values()) {
      values.add(new InstructionContainer(instruction));
    }
    Collections.sort(values, (a, b) -> a.sortKey.compareTo(b.sortKey));
    adapter.updateData(values);
    instructionsView.setAdapter(adapter);

    InstructionsListViewModel activityViewModel =
        ViewModelProviders.of(getActivity()).get(InstructionsListViewModel.class);

    TextView startDate = view.findViewById(R.id.tv_start_date);
    startDate.setText("Unspecified");
    TextView status = view.findViewById(R.id.tv_status);
    status.setText("Unspecified");
    mViewModel.viewState().observe(this, viewState -> {
      Timber.i("Updating ViewState for instructions starting %s", viewState.instructions.startDate);
      startDate.setText(viewState.startDateStr);
      /*LocalDate currentStartDate = viewState.instructions.startDate;
      startDate.setOnClickListener(view1 -> new DatePickerDialog(getContext(), (datePicker, year, month, day) -> {
        mViewModel.updateStartDate(new LocalDate(year, month+1, day), (instructionsToRemove) -> Single.create(emitter -> {
          new AlertDialog.Builder(getActivity())
              .setTitle("Remove Instructions?")
              .setMessage(String.format("This change would overlap %d existing instructions. Should they be dropped?", instructionsToRemove.size()))
              .setPositiveButton("Yes", (d, i) -> {
                emitter.onSuccess(true);
                d.dismiss();
              })
              .setNegativeButton("No", (d, i) -> {
                emitter.onSuccess(false);
                d.dismiss();
              })
              .show();
        })).subscribe();
      }, currentStartDate.getYear(), currentStartDate.getMonthOfYear()-1, currentStartDate.getDayOfMonth()).show());*/

      status.setText(viewState.statusStr);

      for (Instruction instruction : Instruction.values()) {
        ViewHolder holder = adapter.holderForItem(new InstructionContainer(instruction));
        holder.setChecked(viewState.instructions.activeItems.contains(instruction));
      }
      if (!Strings.isNullOrEmpty(viewState.collisionPrompt)) {
        new AlertDialog.Builder(getActivity())
            .setTitle("Invalid Selection")
            .setMessage(viewState.collisionPrompt)
            .setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss())
            .show();
      }
    });

    return view;
  }

  @Override
  public void onDestroy() {
    mViewModel.onCleared();
    super.onDestroy();
  }

  private static final List<InstructionContainer> EXTRA_INSTRUCTIONS = ImmutableList.of(
      new InstructionContainer('D', "Days of fertility (use to achieve pregnancy)"),
      new InstructionContainer('E', "Days of infertility (use to avoid pregnancy)"),
      new InstructionContainer('G', "Double Peak"));

  private static class InstructionContainer {

    public final String sortKey;
    public final String text;
    @Nullable public final Instruction instruction;


    InstructionContainer(Instruction instruction) {
      StringBuilder keyBuilder = new StringBuilder();
      keyBuilder.append(instruction.section);
      if (instruction.subSection != null) {
        keyBuilder.append(".").append(instruction.subSection);
      }
      this.sortKey = keyBuilder.toString();
      this.text = instruction.description;
      this.instruction = instruction;
    }

    InstructionContainer(char section, String text) {
      this.sortKey = String.valueOf(section);
      this.text = text;
      this.instruction = null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o instanceof InstructionContainer) {
        InstructionContainer that = (InstructionContainer) o;
        return Objects.equal(this.sortKey, that.sortKey)
            && Objects.equal(this.text, that.text)
            && Objects.equal(this.instruction, that.instruction);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(sortKey, text, instruction);
    }
  }

  private static class ViewHolder extends SimpleArrayAdapter.SimpleViewHolder<InstructionContainer> {

    private final TextView mKey;
    private final TextView mText;
    private final SwitchCompat mSwitch;

    private InstructionContainer mBoundInstruction;

    public ViewHolder(View view, BiConsumer<Instruction, Boolean> toggleConsumer) {
      super(view);
      mKey = view.findViewById(R.id.tv_instruction_key);
      mText = view.findViewById(R.id.tv_instruction_text);
      mSwitch = view.findViewById(R.id.switch_instruction);
      mSwitch.setOnCheckedChangeListener((compoundButton, checked) -> {
        Preconditions.checkNotNull(mBoundInstruction.instruction);
        try {
          toggleConsumer.accept(mBoundInstruction.instruction, checked);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });
    }

    @Override
    protected void updateUI(InstructionContainer data) {
      mBoundInstruction = data;
      mKey.setText(data.sortKey);
      mText.setText(data.text);
      mSwitch.setVisibility(data.instruction != null ? View.VISIBLE : View.GONE);
      mText.setTypeface(null, data.instruction != null ? Typeface.NORMAL : Typeface.ITALIC);
    }

    void setChecked(boolean checked) {
      if (mSwitch.isChecked() != checked) {
        mSwitch.setChecked(checked);
      }
    }
  }
}
