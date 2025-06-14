package org.ticketMastery.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ticketMastery.entities.Train;
import org.ticketMastery.entities.User;
import org.ticketMastery.utils.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserBookingService {

    private User user = new User();
    private List<User> usersList = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_PATH = "app/src/main/java/org/ticketMastery/localDb/users.json";

    public UserBookingService(User user) throws IOException {
        this.user = user;
        loadUserListFromFile();
    }

    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    private void loadUserListFromFile() throws IOException {
        usersList = objectMapper.readValue(new File(USER_PATH), new TypeReference<List<User>>() {});
    }

    public Boolean loginUser(User user) {
        Optional<User> ourUser = usersList.stream()
                .filter(u -> user.getName().equals(u.getName()) && UserServiceUtil.checkPassword(user.getPassword(), u.getPassword()))
                .findFirst();

        return ourUser.isPresent();
    }

    public Boolean signUpUser(User user) {
        try {
            user.setHashedPassword(UserServiceUtil.hashPassword(user.getPassword()));
            usersList.add(user);
            saveUserListToFile();
            return true;
        } catch (IOException e) {
            System.out.println("Error signing up user: " + e.getMessage());
            return false;
        }

    }

    private void saveUserListToFile() throws IOException {
        objectMapper.writeValue(new File(USER_PATH), usersList);
    }

    public void fetchUserTickets() {
        user.printTickets();
    }

    public AtomicBoolean cancelTicket(String ticketId) {
        AtomicBoolean ticketCancelled = new AtomicBoolean(false); // to track if cancellation happened

        usersList.stream()
                .filter(user -> user.getTicketsBooked().stream()
                        .anyMatch(ticket -> ticket.getTicketId().equals(ticketId)))
                .findFirst()
                .ifPresent(user -> {
                    ticketCancelled.set(user.getTicketsBooked().removeIf(ticket -> ticket.getTicketId().equals(ticketId)));
                    try {
                        saveUserListToFile();
                    } catch (IOException e) {
                        System.out.println("Error saving user tickets: " + e.getMessage());
                    }
                });

        return ticketCancelled; // return whether cancellation was successful
    }

    public List<Train> getTrains(String source, String destination){
        try{
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        }catch(IOException ex){
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train){
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat) {
        try{
            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);
                    trainService.addTrain(train);
                    return true; // Booking successful
                } else {
                    return false; // Seat is already booked
                }
            } else {
                return false; // Invalid row or seat index
            }
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }
}