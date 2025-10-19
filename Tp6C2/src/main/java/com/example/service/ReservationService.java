package com.example.service;

import com.example.model.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationService {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findAll();
    void update(Reservation reservation);
    void delete(Reservation reservation);
    boolean isSalleAvailable(Long salleId, Reservation reservation);
}