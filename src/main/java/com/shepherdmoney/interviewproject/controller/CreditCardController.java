package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.*;

@RestController
public class CreditCardController {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> optionalUser = userRepository.findById(payload.getUserId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setOwner(user);
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            // Use a stream to convert the list of credit cards to a list of CreditCardView
            List<CreditCardView> creditCardViews = optionalUser.get().getCreditCards().stream()
                    .map(CreditCardView::fromCreditCard)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(creditCardViews);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (optionalCreditCard.isPresent()) {
            return ResponseEntity.ok(optionalCreditCard.get().getOwner().getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Void> updateBalances(@RequestBody UpdateBalancePayload[] payload) {
        Arrays.sort(payload, Comparator.comparing(UpdateBalancePayload::getTransactionTime));
        // Round all transaction times to the start of the day
        for (UpdateBalancePayload transaction : payload) {
            Instant transactionTime = transaction.getTransactionTime();
            Instant startOfDay = transactionTime.truncatedTo(ChronoUnit.DAYS);
            transaction.setTransactionTime(startOfDay);
        }

        for (UpdateBalancePayload transaction : payload) {
            String creditCardNumber = transaction.getCreditCardNumber();
            Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(creditCardNumber);

            if (!optionalCreditCard.isPresent()) {
                return ResponseEntity.badRequest().build();
            }

            CreditCard creditCard = optionalCreditCard.get();
            Instant transactionTime = transaction.getTransactionTime();
            Instant currentTime = Instant.now();
            double balanceToAdd = transaction.getTransactionAmount();

            List<BalanceHistory> balanceHistory = creditCard.getBalanceHistory();

            /* In each day, if there's a balanceHistory with the same day in the list, add transactionAmount to the balance
            of that balanceHistory. Otherwise, add a new balanceHistory.*/
            while (!transactionTime.isAfter(currentTime)) {
                final Instant dayToCheck = transactionTime;
                Optional<BalanceHistory> existingBalanceHistory = balanceHistory.stream()
                        .filter(bh -> bh.getDate().truncatedTo(ChronoUnit.DAYS).equals(dayToCheck))
                        .findFirst();

                if (existingBalanceHistory.isPresent()) {
                    existingBalanceHistory.get().setBalance(existingBalanceHistory.get().getBalance() + balanceToAdd);
                } else {
                    balanceHistory.add(new BalanceHistory(transactionTime, balanceToAdd));
                }

                transactionTime = transactionTime.plus(1, ChronoUnit.DAYS);
            }

            creditCardRepository.save(creditCard);
        }

        return ResponseEntity.ok().build();
    }
}
