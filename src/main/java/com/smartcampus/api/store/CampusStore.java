package com.smartcampus.api.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.exception.SensorUnavailableException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

public final class CampusStore {
    private static final CampusStore INSTANCE = new CampusStore();
    private static final String ACTIVE = "ACTIVE";

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<SensorReading>> readingsBySensor = new ConcurrentHashMap<>();

    private CampusStore() {
        seedData();
    }

    public static CampusStore getInstance() {
        return INSTANCE;
    }

    public synchronized void reset() {
        rooms.clear();
        sensors.clear();
        readingsBySensor.clear();
        seedData();
    }

    private void seedData() {
        Room library = new Room("LIB-301", "Library Quiet Study", 30);
        Room lab = new Room("LAB-210", "Networking Lab", 24);
        Room hall = new Room("HALL-100", "Main Lecture Hall", 120);
        rooms.put(library.getId(), library);
        rooms.put(lab.getId(), lab);
        rooms.put(hall.getId(), hall);

        addSeedSensor(new Sensor("TEMP-001", "Temperature", ACTIVE, 22.4, library.getId()));
        addSeedSensor(new Sensor("CO2-001", "CO2", ACTIVE, 415.0, library.getId()));
        addSeedSensor(new Sensor("OCC-009", "Occupancy", "MAINTENANCE", 0.0, lab.getId()));
        readingsBySensor.get("TEMP-001").add(new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis(), 22.4));
        readingsBySensor.get("CO2-001").add(new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis(), 415.0));
    }

    private void addSeedSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readingsBySensor.put(sensor.getId(), new ArrayList<>());
        rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
    }

    public List<Room> getRooms() {
        return rooms.values().stream()
                .map(Room::new)
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());
    }

    public Optional<Room> getRoom(String roomId) {
        Room room = rooms.get(roomId);
        return room == null ? Optional.empty() : Optional.of(new Room(room));
    }

    public synchronized Room createRoom(Room request) {
        Room room = new Room(request.getId(), request.getName(), request.getCapacity());
        room.setSensorIds(request.getSensorIds());
        rooms.put(room.getId(), room);
        return new Room(room);
    }

    public synchronized void deleteRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted because active hardware is still assigned.");
        }
        rooms.remove(roomId);
    }

    public List<Sensor> getSensors(String type) {
        return sensors.values().stream()
                .filter(sensor -> matchesType(sensor, type))
                .map(Sensor::new)
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    private boolean matchesType(Sensor sensor, String type) {
        if (type == null || type.trim().isEmpty()) {
            return true;
        }
        return sensor.getType() != null && sensor.getType().toLowerCase(Locale.ROOT).equals(type.trim().toLowerCase(Locale.ROOT));
    }

    public Optional<Sensor> getSensor(String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        return sensor == null ? Optional.empty() : Optional.of(new Sensor(sensor));
    }

    public synchronized Sensor createSensor(Sensor request) {
        Room room = rooms.get(request.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room " + request.getRoomId() + " does not exist, so the sensor cannot be registered.");
        }

        Sensor sensor = new Sensor(request.getId(), request.getType(), request.getStatus(), request.getCurrentValue(), request.getRoomId());
        sensors.put(sensor.getId(), sensor);
        readingsBySensor.putIfAbsent(sensor.getId(), new ArrayList<>());
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }
        return new Sensor(sensor);
    }

    public synchronized List<SensorReading> getReadings(String sensorId) {
        if (!sensors.containsKey(sensorId)) {
            return null;
        }
        return readingsBySensor.getOrDefault(sensorId, List.of()).stream()
                .map(SensorReading::new)
                .sorted(Comparator.comparingLong(SensorReading::getTimestamp))
                .collect(Collectors.toList());
    }

    public synchronized SensorReading addReading(String sensorId, double value) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return null;
        }
        if (!ACTIVE.equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is in " + sensor.getStatus() + " state and cannot accept readings.");
        }
        SensorReading reading = new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis(), value);
        readingsBySensor.computeIfAbsent(sensorId, ignored -> new ArrayList<>()).add(reading);
        sensor.setCurrentValue(value);
        return new SensorReading(reading);
    }
}
