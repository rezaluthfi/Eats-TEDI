import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eatstedi.databinding.ViewItemScheduleBinding
import com.example.eatstedi.model.Schedule

class ScheduleAdapter(private val schedules: MutableList<Schedule>, private val onDelete: (Int) -> Unit) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(private val binding: ViewItemScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(schedule: Schedule, position: Int) {
            binding.tvDay.text = schedule.day
            binding.tvShift.text = schedule.shift

            // Menangani klik pada tombol hapus
            binding.ivDelete.setOnClickListener {
                println("Before removal: ${schedules.size}") // Log jumlah sebelum penghapusan
                onDelete(adapterPosition) // Gunakan adapterPosition untuk mendapatkan posisi yang benar
                println("After removal: ${schedules.size}") // Log jumlah setelah penghapusan
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ViewItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position], position)
    }

    override fun getItemCount(): Int = schedules.size

    fun addSchedule(schedule: Schedule) {
        schedules.add(schedule)
        notifyItemInserted(schedules.size - 1)
    }
}

