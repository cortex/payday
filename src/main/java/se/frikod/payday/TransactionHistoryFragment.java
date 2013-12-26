/*
*
*   Copyright (C) 2012-2013 Joakim Lundborg <joakim,lundborg@gmail.com>
*
*     This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package se.frikod.payday;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class TransactionHistoryFragment extends Fragment {
    private static final String TAG = TransactionHistoryFragment .class.getName();
    private TransactionsGraphView tv;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.transaction_history_fragment, container, false);
        assert mainView != null;
        tv = (TransactionsGraphView) mainView.findViewById(R.id.transactionGraph);
        update();
        return mainView;
    }

    private void update(){
        BankdroidProvider bank = ((PaydayActivity) this.getActivity()).bank;
        List<Transaction> transactions = bank.getTransactions();
        tv.setTransactions(transactions);
        tv.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    public void onSelected(){
        tv.mRenderer.initialAnimation();
    }

}
