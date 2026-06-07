package com.mathewshereni.open_aid_transparency.recipient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A country (or region) that receives aid. In our domain these are the
 * aid-receiving nations, e.g. across Sub-Saharan Africa.
 */
@Entity
@Table(name = "recipients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "country_name", nullable = false, unique = true, length = 100)
    private String countryName;


    @Column(name = "iso_code", unique = true, length = 3)
    private String isoCode;


    @Column(length = 100)
    private String region;
}
