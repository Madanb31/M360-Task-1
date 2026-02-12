package com.madan.M360_Task_1.controllers;

import com.madan.M360_Task_1.models.Address;
import com.madan.M360_Task_1.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    @Autowired
    private AddressRepository addressRepository;

    @GetMapping()
    public ResponseEntity<List<Address>> getAllAddress(){
        return ResponseEntity.ok(addressRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(
            @PathVariable UUID id
    ){
        Address address = addressRepository.findById(id)
                .orElseThrow( () ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Address not found with id " + id
                        )
                );
        return ResponseEntity.ok(address);
    }

}
