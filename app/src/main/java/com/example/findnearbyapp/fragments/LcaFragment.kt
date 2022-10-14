package com.example.findnearbyapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findnearbyapp.*
import com.example.findnearbyapp.databinding.FragmentLcaBinding
import com.example.findnearbyapp.others.Event
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class LcaFragment : Fragment() {
    private var eventsData: ArrayList<Event> = arrayListOf()
    // Firestore instance
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _binding: FragmentLcaBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLcaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Firestore instance
        db = FirebaseFirestore.getInstance()

        // Create a reference to the cities collection
        val events = db.collection("events").get()
            .addOnSuccessListener {
                // First clear the data
                eventsData.clear()

                for (document in it) {
                    val data = Event(
                        id = document.id,
                        title = document.data["title"].toString(),
                        description = if (document.data["description"] == null) "No description" else document.data["description"].toString(),
                        latitude = document.data["latitude"] as Double,
                        longitude = document.data["longitude"] as Double,
                        radius = document.data["radius"].toString().toInt(),
                        authorId = document.data["authorId"].toString(),
                        timestamp = document.data["timestamp"] as Timestamp
                    )
                    // Add the data to the list
                    eventsData.add(data)
                }

                initData()
            }
    }

    private fun initData() {
        // plop the dummy data into the adapter
        val adapter = this.context?.let { MainCardAdapter(it, eventsData) }

        // finally plop the adapter into the RV (recycler view)
        binding.lcaRv.adapter = adapter
        binding.lcaRv.layoutManager = LinearLayoutManager(this.context)

        // When item on Recyclerview invoke an onItemClick event from the adapter,
        // the adapter will also invoke this custom callback
        adapter?.onItemClick = {
            // GO TO NEARBY FRAGMENT by REPLACING THE HOST FRAGMENT CONTAINER
            val fragment = NearbyFragment()

            // Pass the Lat and Long
            val args = Bundle()
            args.putDouble("focusLat", it.latitude)
            args.putDouble("focusLong", it.longitude)
            fragment.arguments = args

            // SET BOTTOM NAV ACTIVE MENU TO 1
            val activity: MainActivity = activity as MainActivity
            activity.setActiveMenu(1)

            // GO TO NEARBY FRAGMENT
            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(R.id.hostFragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}