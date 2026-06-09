package com.mathewshereni.open_aid_transparency.funding;

import com.mathewshereni.open_aid_transparency.common.exception.ResourceNotFoundException;
import com.mathewshereni.open_aid_transparency.donor.Donor;
import com.mathewshereni.open_aid_transparency.donor.DonorRepository;
import com.mathewshereni.open_aid_transparency.project.AidProject;
import com.mathewshereni.open_aid_transparency.project.AidProjectRepository;
import com.mathewshereni.open_aid_transparency.recipient.Recipient;
import com.mathewshereni.open_aid_transparency.recipient.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingFlowService {

    private final FundingFlowRepository repository;
    private final DonorRepository donorRepository;
    private final RecipientRepository recipientRepository;
    private final AidProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<FundingFlowResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "year"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FundingFlowResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /** The aggregation report: total funding grouped by recipient country + currency. */
    @Transactional(readOnly = true)
    public List<FundingByRecipient> totalByRecipient() {
        return repository.totalByRecipient();
    }

    /** Total funding grouped by SDG category (only flows tagged with an SDG). */
    @Transactional(readOnly = true)
    public List<FundingBySdg> totalBySdg() {
        return repository.totalBySdg();
    }

    /** Total funding per year (trend). */
    @Transactional(readOnly = true)
    public List<FundingByYear> totalByYear() {
        return repository.totalByYear();
    }

    /** Total funding per donor (ranking). */
    @Transactional(readOnly = true)
    public List<FundingByDonor> totalByDonor() {
        return repository.totalByDonor();
    }

    @Transactional
    public FundingFlowResponse create(FundingFlowRequest request) {
        FundingFlow flow = FundingFlow.builder()
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .year(request.year())
                .transactionDate(request.transactionDate())
                .donor(resolveDonor(request.donorId()))
                .recipient(resolveRecipient(request.recipientId()))
                .project(resolveProject(request.projectId()))
                .build();
        return toResponse(repository.save(flow));
    }

    @Transactional
    public FundingFlowResponse update(Long id, FundingFlowRequest request) {
        FundingFlow flow = getOrThrow(id);
        flow.setAmount(request.amount());
        flow.setCurrency(request.currency().toUpperCase());
        flow.setYear(request.year());
        flow.setTransactionDate(request.transactionDate());
        flow.setDonor(resolveDonor(request.donorId()));
        flow.setRecipient(resolveRecipient(request.recipientId()));
        flow.setProject(resolveProject(request.projectId()));
        return toResponse(repository.save(flow));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    // ---------- helpers ----------

    private Donor resolveDonor(Long donorId) {
        return donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Donor " + donorId + " not found."));
    }

    private Recipient resolveRecipient(Long recipientId) {
        return recipientRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient " + recipientId + " not found."));
    }

    /** Optional: only resolve when a projectId was supplied. */
    private AidProject resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Aid project " + projectId + " not found."));
    }

    private FundingFlow getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funding flow " + id + " not found."));
    }

    private FundingFlowResponse toResponse(FundingFlow f) {
        FundingFlowResponse.ProjectSummary project = (f.getProject() == null)
                ? null
                : new FundingFlowResponse.ProjectSummary(f.getProject().getId(), f.getProject().getTitle());

        return new FundingFlowResponse(
                f.getId(),
                f.getAmount(),
                f.getCurrency(),
                f.getYear(),
                f.getTransactionDate(),
                new FundingFlowResponse.DonorSummary(f.getDonor().getId(), f.getDonor().getName()),
                new FundingFlowResponse.RecipientSummary(f.getRecipient().getId(), f.getRecipient().getCountryName()),
                project);
    }
}
