package com.jantonioc.xallyapp.FragmentsPedidos;


import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jantonioc.ln.Orden;
import com.jantonioc.xallyapp.Adaptadores.PedidosAdapter;
import com.jantonioc.xallyapp.FragmentsOrdenes.Categorias;
import com.jantonioc.xallyapp.FragmentsOrdenes.DetalleMenu;
import com.jantonioc.xallyapp.FragmentsOrdenes.Ordenes;
import com.jantonioc.xallyapp.MainActivity;
import com.jantonioc.xallyapp.R;
import com.jantonioc.xallyapp.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class Pedidos extends Fragment {

    private View rootView;
    private RecyclerView lista;
    private ProgressBar progressBar;
    private List<Orden> listaPedidos;

    private PedidosAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;

    public Pedidos() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //cambiar el nombre del toolbar
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Pedidos");

        //ocultar el floating boton
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.hide();

        //vista del fragment
        rootView = inflater.inflate(R.layout.fragment_pedidos, container, false);
        //recycler view
        lista = rootView.findViewById(R.id.recyclerViewPedidos);
        lista.setHasFixedSize(true);
        lista.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        //progressbar
        progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        //swipe to refresh
        swipeRefreshLayout = rootView.findViewById(R.id.swipe);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                listaPedidos();
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);

            }
        });

        //listar los pedidos
        listaPedidos();


        // Inflate the layout for this fragment
        return rootView;
    }

    private void listaPedidos()
    {
        //limpiar los pedidos al consultar al WS
        listaPedidos= new ArrayList<>();

        String uri = "http://192.168.1.52/MenuAPI/API/OrdenesWS/Ordenes";
        StringRequest request = new StringRequest(Request.Method.GET, uri, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    //obteniendo el arreglo desde la respuesta
                    JSONArray jsonArray = new JSONArray(response);

                    //Recorriendo el arreglo paara obtener los objetos
                    for (int i = 0; i < jsonArray.length(); i++) {

                        //Obteniendo los objetos
                        JSONObject obj = jsonArray.getJSONObject(i);

                        //obteniendo la fecha en Date
                        String jsondate = obj.getString("fechaorden");

                        //Convirtiendo la fecha a legible
                        String fecha = ConvertirJsonFecha(jsondate);

                        //Convirtiendo el tiempo a legible
                        String hora = ConvertirJsonTiempo(jsondate);

                        //Obteniendo los datos y convirtiendolos a Orden
                        Orden orden = new Orden(
                                obj.getInt("id"),
                                obj.getString("codigo"),
                                fecha,
                                hora,
                                obj.getBoolean("estado")
                        );

                        //Agregando a la lista de orden
                        listaPedidos.add(orden);
                    }

                    //Si la lista es mayor que 0 adaptamos y hacemos el evento on click
                    if (listaPedidos.size() > 0) {

                        progressBar.setVisibility(View.GONE);

                        adapter = new PedidosAdapter(listaPedidos);

                        adapter.setClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                    if(listaPedidos.get(lista.getChildAdapterPosition(v)).getId()!=MainActivity.orden.getId())
                                    {
                                        MainActivity.listadetalle.clear();
                                    }

                                detalleOrden(listaPedidos.get(lista.getChildAdapterPosition(v)).getId());
                            }
                        });

                        lista.setAdapter(adapter);

                    }
                    //Si no es mayor regresamos al fragmento anterior y sacamos el fragment actual de la pila
                    else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(rootView.getContext(), "No se poseen ordenes para el dia de hoy", Toast.LENGTH_SHORT).show();

                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                            fm.popBackStack();
                        }

                        Fragment fragment = new Ordenes();
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.content, fragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }

                } catch (JSONException ex) {

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(rootView.getContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                    ex.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(rootView.getContext(), error.getMessage(), Toast.LENGTH_LONG).show();

            }
        });


        VolleySingleton.getInstance(rootView.getContext()).addToRequestQueue(request);


    }


    private void detalleOrden(int idorden)
    {
        //Abrir el fragmento del detalle de Orden
        Fragment fragment = new DetallesDeOrden();
        //Pasar parametros entre fragment
        Bundle bundle = new Bundle();
        //mandar el objeto serializado
        bundle.putInt("idorden",idorden);
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }



    //obtener la fecha en forato legible para el usuario
    private static String ConvertirJsonFecha(String jsonfecha)
    {

        jsonfecha=jsonfecha.replace("/Date(", "").replace(")/", "");
        long tiempo = Long.parseLong(jsonfecha);
        Date fecha= new Date(tiempo);

        return new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    //obtener el tiempo en formato legible para el usuario
    private static String ConvertirJsonTiempo(String jsonfecha)
    {
        jsonfecha=jsonfecha.replace("/Date(", "").replace(")/", "");
        long tiempo = Long.parseLong(jsonfecha);
        Date fecha= new Date(tiempo);

        return new SimpleDateFormat("hh:mm a").format(fecha);
    }



}
