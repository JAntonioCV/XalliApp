package com.jantonioc.xalliapp.FragmentsComanda;


import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jantonioc.ln.DetalleDeOrden;
import com.jantonioc.xalliapp.Constans;
import com.jantonioc.xalliapp.FragmentsPedidos.DetallesDeOrden;
import com.jantonioc.xalliapp.MainActivity;
import com.jantonioc.xalliapp.VolleySingleton;
import com.jantonioc.xalliapp.Adaptadores.DetalleOrdenAdapter;
import com.jantonioc.xalliapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetallesComanda extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    //interfaz
    private View rootView;
    private RecyclerView lista;
    private DetalleOrdenAdapter adapter;
    private ProgressBar progressBar;
    private List<DetalleDeOrden> listadetalle;

    private Button btnenviar;
    private TextView total;

    private int idorden;

    //swipe to refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    private RelativeLayout relativeLayout;
    private RelativeLayout noconection;
    private FloatingActionButton fab;

    private Button btnreintentar;
    private TextView txterror;



    public DetallesComanda() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //toolbar
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Detalle Ordenes");

        //fab boton
        fab = getActivity().findViewById(R.id.fab);
        fab.hide();

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_detalles_de_orden, container, false);

        relativeLayout = rootView.findViewById(R.id.relative);
        noconection = rootView.findViewById(R.id.noconection);
        relativeLayout.setVisibility(View.GONE);

        //de la vista de no conexion
        btnreintentar = rootView.findViewById(R.id.btnrein);
        txterror = rootView.findViewById(R.id.errorTitle);

        //progressbar
        progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        lista = rootView.findViewById(R.id.recyclerViewDetalleOrden);
        lista.setHasFixedSize(true);
        lista.setLayoutManager(new LinearLayoutManager(rootView.getContext()));

        total = rootView.findViewById(R.id.total);

        //Validar si la lista tiene datos envia si no, muestra un mensaje
        btnenviar = rootView.findViewById(R.id.btnenviar);
        btnenviar.setText("Guardar Comanda");

        //abrir el agregar comanda
        btnenviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressBar.setVisibility(View.VISIBLE);
                relativeLayout.setVisibility(View.GONE);
                lista.setVisibility(View.GONE);

                Fragment fragment = new AgregarComanda();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

            }
        });

        //obteniendo el id de la orden selecionada
        Bundle bundle = getArguments();
        idorden = bundle.getInt("idOrden",0);

        //swipe to refresh
        swipeRefreshLayout = rootView.findViewById(R.id.swipe);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        swipeRefreshLayout.setOnRefreshListener(this);

        btnreintentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        DetallesComanda.this.obtenerDetalles(idorden);
                    }
                });
            }
        });

        //obtenemos los detalles
        obtenerDetalles(idorden);

        return rootView;

    }

    private void obtenerDetalles(int idorden) {

        swipeRefreshLayout.setRefreshing(true);
        relativeLayout.setVisibility(View.GONE);
        noconection.setVisibility(View.GONE);
        lista.setVisibility(View.GONE);

        listadetalle = new ArrayList<>();

        String uri = Constans.URLBASE+"DetallesDeOrdenWS/DetalleDeOrdenCuenta/" + idorden;
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

                        //Obteniendo los datos y convirtiendolos a Detalle orden
                        DetalleDeOrden detalleDeOrden = new DetalleDeOrden(
                                obj.getInt("id"),
                                obj.getInt("cantidadorden"),
                                obj.getString("notaorden"),
                                obj.getString("nombreplatillo"),
                                obj.getBoolean("estado"),
                                obj.getDouble("preciounitario"),
                                obj.getInt("menuid"),
                                obj.getBoolean("fromservice")
                        );

                        //Agregando a la lista del menu
                        listadetalle.add(detalleDeOrden);
                    }

                    //Si la lista es mayor que 0 adaptamos y hacemos el evento on click y long Click del la lista
                    if (listadetalle.size() > 0) {

                        relativeLayout.setVisibility(View.VISIBLE);
                        lista.setVisibility(View.VISIBLE);
                        swipeRefreshLayout.setRefreshing(false);

                        //Calcular el total
                        total.setText("$" + calcularTotal(listadetalle));

                        adapter = new DetalleOrdenAdapter(listadetalle);

                        lista.setAdapter(adapter);

                    }
                    //Si no es mayor regresamos al fragmento anterior y sacamos el fragment actual de la pila
                    else {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(rootView.getContext(), "Esta Orden no tiene detalles", Toast.LENGTH_SHORT).show();

                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                            fm.popBackStack();
                        }

                        //acordarse de abrir la vista anterior
                        Fragment fragment = new OrdenComanda();
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.content, fragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }

                } catch (JSONException ex) {

                    //excepcion json
                    swipeRefreshLayout.setRefreshing(false);
                    noconection.setVisibility(View.VISIBLE);
                    Toast.makeText(rootView.getContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                swipeRefreshLayout.setRefreshing(false);
                noconection.setVisibility(View.VISIBLE);
                Toast.makeText(rootView.getContext(),Constans.errorVolley(error), Toast.LENGTH_SHORT).show();
            }
        })
        {
            //metodo para la autenficacion basica en el servidor
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return MainActivity.getToken();
            }
        };

        VolleySingleton.getInstance(rootView.getContext()).addToRequestQueue(request);
    }

    //Calclulamos el total del detalle para la orden
    private String calcularTotal(List<DetalleDeOrden> detalleDeOrdens) {

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(2);

        double total = 0;

        for (DetalleDeOrden detalleActual : detalleDeOrdens) {
            total = total + detalleActual.getCantidad() * detalleActual.getPrecio();
        }
        return format.format(total);
    }

    @Override
    public void onRefresh() {
        obtenerDetalles(idorden);
    }
}
