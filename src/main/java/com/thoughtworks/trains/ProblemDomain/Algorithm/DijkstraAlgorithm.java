package com.thoughtworks.trains.ProblemDomain.Algorithm;

import com.thoughtworks.trains.Exception.RouteNotFoundException;
import com.thoughtworks.trains.ProblemDomain.City;
import com.thoughtworks.trains.ProblemDomain.Edge;
import com.thoughtworks.trains.ProblemDomain.Respository.CityRepository;
import com.thoughtworks.trains.ProblemDomain.Respository.EdgeRepository;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * DijkstraAlgorithm Algorithm is mainly used to find the shortest route between two points on a graph
 * See <a href="https://www.youtube.com/watch?v=2E7MmKv0Y24">MIT professor explaining Dijkstra's Algorithm </a>
 */
public class DijkstraAlgorithm implements ITrainsAlgorithm {

    private EdgeRepository edgeRepository;

    private CityRepository cityRepository;

    private Graph graph = new Graph();

    private Stack<City> citiesStack = new Stack<>();

    /**
     * @param edgeRepository repository of all the edges extracted from the problem statement
     * @param cityRepository repository of all the cities extracted from the problem statement
     */
    public DijkstraAlgorithm(EdgeRepository edgeRepository, CityRepository cityRepository)
    {
        this.edgeRepository = edgeRepository;

        this.cityRepository = cityRepository;
    }

    /**
     * {@inheritDoc}
     */
    public int findLengthOfRoute(ArrayList<String> cityNames) throws RouteNotFoundException
    {
        ArrayList<City> routeCities = cityNames
                                        .stream()
                                        .map(city -> this.cityRepository.getCityByName(city))
                                        .collect(Collectors.toCollection(ArrayList::new));

        discoverGraphAtSourceCity(routeCities.get(0), 0);

        Route route = this.graph.findRoute(routeCities);

        if((route == null ))
        {
            throw new RouteNotFoundException("NO SUCH ROUTE");
        }
        return  route.getLength();
    }

    /**
     * {@inheritDoc}
     */
    public int findLengthOfShortestRoute(String start, String end) throws RouteNotFoundException
    {
        try {
            return findLengthOfShortestRoute(
                    this.cityRepository.getCityByName(start),
                    this.cityRepository.getCityByName(end)
            );
        }catch (Exception e){
            throw new RouteNotFoundException("NO SUCH ROUTE");
    }
}

    /**
     * {@inheritDoc}
     */
    public int findNumberOfTrips(
            String startCity,
            String endCity,
            ComparisonOperation operation,
            Operator operator,
            int operandValue
    ) throws RouteNotFoundException
    {
        try {
            return filterAllPossibleRoutes(
                    this.cityRepository.getCityByName(startCity),
                    this.cityRepository.getCityByName(endCity),
                    operation,
                    operator,
                    operandValue
            );
        } catch (Exception e) {
            throw new RouteNotFoundException("NO SUCH ROUTE");
        }
    }

    /**
     * @param city source city to start the path discovery from
     * @param routeLengthLimit the limit of the length of the discovered route (search should stop ar this limit)
     */
    private void discoverGraphAtSourceCity(City city, int routeLengthLimit)
    {
        this.traversePath(city, city, 0, routeLengthLimit);
    }

    /***
     * @param currentCity
     * @param sourceCity
     * @param distanceFromSource
     * @param routeLengthLimit
     */
    private void traversePath(City currentCity, City sourceCity, int distanceFromSource, int routeLengthLimit)
    {
        this.citiesStack.push(currentCity);

        // getting all edges that start with this city to dig them down $$
        ArrayList<Edge> edgesFromCurrentCity = this.edgeRepository.fetchByStartCity(currentCity);

        if(!edgesFromCurrentCity.isEmpty())
        {

            for (Edge edge : edgesFromCurrentCity) {
                int routeLength = distanceFromSource + edge.getEdgeLength();

                City nextCity = cityRepository.getCityByName(edge.getEnd().getCityName());

                // Register current route possible route
                graph.registerRoute(
                        this.citiesStack,
                        nextCity,
                        sourceCity,
                        routeLength
                );

                switch (routeLengthLimit) {
                    case 0:
                        // Avoid going through circular paths
                        if (!this.isCircularPath(nextCity)) {
                            this.traversePath(nextCity, sourceCity, routeLength, routeLengthLimit);
                        }
                        break;

                    default:
                        if (routeLength < routeLengthLimit) {
                            this.traversePath(nextCity, sourceCity, routeLength, routeLengthLimit);
                        }
                }
            }
        }
        // popping the visited city from the stack as all its possible routes have been discovered
        this.citiesStack.pop();
    }

    /**
     * @param start
     * @param end
     * @return
     * @throws RouteNotFoundException
     */
    private int findLengthOfShortestRoute(City start, City end) throws RouteNotFoundException
    {
        discoverGraphAtSourceCity(start, 0);

        Route shortestRoute = null;

        for(Map.Entry<String, Route> entry : this.graph.getRoutesList().entrySet())
        {
            Route route = entry.getValue();

            if(route.getStart().equals(start) &&  route.getEnd().equals(end) )
            {
                if(shortestRoute == null)
                {
                    shortestRoute = route;
                }
                else
                {
                    if(route.getLength() < shortestRoute.getLength()){
                        shortestRoute = route;
                    }
                }
            }
        }

        if(shortestRoute == null){
            throw new RouteNotFoundException("NO SUCH ROUTE");
        }
        return shortestRoute.getLength();
    }

    /**
     * @param startCity city where the route (trip) discovery should start from
     * @param endCity city where the route (trip) discovery should end at
     * @param operation
     * @param operator
     * @param operand
     * @return
     */
    private int filterAllPossibleRoutes(City startCity, City endCity, ComparisonOperation operation, Operator operator, int operand)
    {
        switch (operation){
            case LENGTH:
                discoverGraphAtSourceCity(startCity, operand);
                break;

            case NUMBER_OF_STOPS:
                discoverGraphAtSourceCity(startCity, 0);
                break;
        }

        ArrayList<Route> routes = new ArrayList<>();

        for(Map.Entry<String, Route> entry : this.graph.getRoutesList().entrySet())
        {
            Route route = entry.getValue();

            boolean condition = false;
            int leftOperand = 0;

            switch (operation){

                case LENGTH:
                    leftOperand = route.getLength();
                    break;

                case NUMBER_OF_STOPS:
                    leftOperand = route.getNumberOfStops();
                    break;
            }

            switch (operator)
            {
                case EQUAL:
                    condition = (leftOperand == operand);
                    break;

                case EQUAL_ORL_ESS:
                    condition = (leftOperand <= operand);
                    break;

                case EQUAL_OR_MORE:
                    condition = (leftOperand >= operand);
                    break;

                case LESS_THAN:
                    condition = (leftOperand < operand);
                    break;

                case MORE_THAN:
                    condition = (leftOperand > operand);
                    break;
            }

            if(route.getStart().equals(startCity) &&  route.getEnd().equals(endCity) && (condition))
            {
                routes.add(route);
            }
        }
        return routes.size();
    }

    /**
     * @param city next city to visit
     * @return true if the city will lead to a circular path (eg: A-B-C-A-B-C), returns false otherwise
     */
    private boolean isCircularPath(City city)
    {
        for (City n : this.citiesStack){
            if(n.getCityName().equals(city.getCityName())){
                return true;
            }
        }
        return false;
    }
}