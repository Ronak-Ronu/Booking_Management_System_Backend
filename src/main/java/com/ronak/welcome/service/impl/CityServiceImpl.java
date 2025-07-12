package com.ronak.welcome.service.impl;

import com.ronak.welcome.entity.City;
import com.ronak.welcome.entity.Country;
import com.ronak.welcome.entity.State;
import com.ronak.welcome.repository.CityRepository;
import com.ronak.welcome.repository.CountryRepository;
import com.ronak.welcome.repository.StateRepository;
import com.ronak.welcome.service.CityService;
import org.springframework.stereotype.Service;

@Service
public class CityServiceImpl implements CityService {
    private final CountryRepository countryRepo;
    private final StateRepository stateRepo;
    private final CityRepository cityRepo;

    public CityServiceImpl(CountryRepository countryRepo, StateRepository stateRepo, CityRepository cityRepo) {
        this.countryRepo = countryRepo;
        this.stateRepo = stateRepo;
        this.cityRepo = cityRepo;
    }

    @Override
    public City resolveCity(City inputCity) {
        Country country = countryRepo.findByNameIgnoreCase(inputCity.getState().getCountry().getName())
                .orElseGet(() -> countryRepo.save(inputCity.getState().getCountry()));

        State state = stateRepo.findByNameIgnoreCaseAndCountry(inputCity.getState().getName(), country)
                .orElseGet(() -> {
                    inputCity.getState().setCountry(country);
                    return stateRepo.save(inputCity.getState());
                });

        return cityRepo.findByNameIgnoreCaseAndState(inputCity.getName(), state)
                .orElseGet(() -> {
                    inputCity.setState(state);
                    return cityRepo.save(inputCity);
                });
    }

}
