import { configureStore, ThunkAction, Action } from '@reduxjs/toolkit';
import composedSlice from './store/composed-slice';
import explorerSlice from './store/explorer-slice';
import kafkaItemsFlatSlice from './store/kafka-items-flat-slice';


export const store = configureStore({
  reducer: {
    composed: composedSlice,
    explorer: explorerSlice,
    kafkaComposedFlat: kafkaItemsFlatSlice
  },
});

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;
export type AppThunk<ReturnType = void> = ThunkAction<
  ReturnType,
  RootState,
  unknown,
  Action<string>
>;