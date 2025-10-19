package com.example.service;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReservationServiceImpl implements ReservationService {

    private final EntityManagerFactory emf;

    public ReservationServiceImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Reservation save(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Validation de la disponibilité de la salle
            if (!isSalleAvailable(reservation.getSalle().getId(), reservation)) {
                throw new RuntimeException("La salle n'est pas disponible pour cette période");
            }

            em.persist(reservation);
            em.getTransaction().commit();
            return reservation;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            Reservation reservation = em.find(Reservation.class, id);
            return Optional.ofNullable(reservation);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Reservation> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Reservation> query = em.createQuery("SELECT r FROM Reservation r", Reservation.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public void update(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Validation de la disponibilité de la salle (en excluant la réservation actuelle)
            if (!isSalleAvailableForUpdate(reservation.getSalle().getId(), reservation)) {
                throw new RuntimeException("La salle n'est pas disponible pour cette période");
            }

            em.merge(reservation);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            if (!em.contains(reservation)) {
                reservation = em.merge(reservation);
            }
            em.remove(reservation);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public boolean isSalleAvailable(Long salleId, Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT COUNT(r) FROM Reservation r " +
                    "WHERE r.salle.id = :salleId " +
                    "AND r.dateDebut < :end " +
                    "AND r.dateFin > :start";

            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("salleId", salleId);
            query.setParameter("start", reservation.getDateDebut());
            query.setParameter("end", reservation.getDateFin());

            Long count = query.getSingleResult();
            return count == 0;
        } finally {
            em.close();
        }
    }

    private boolean isSalleAvailableForUpdate(Long salleId, Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT COUNT(r) FROM Reservation r " +
                    "WHERE r.salle.id = :salleId " +
                    "AND r.id != :reservationId " +
                    "AND r.dateDebut < :end " +
                    "AND r.dateFin > :start";

            TypedQuery<Long> query = em.createQuery(jpql, Long.class);
            query.setParameter("salleId", salleId);
            query.setParameter("reservationId", reservation.getId());
            query.setParameter("start", reservation.getDateDebut());
            query.setParameter("end", reservation.getDateFin());

            Long count = query.getSingleResult();
            return count == 0;
        } finally {
            em.close();
        }
    }
}